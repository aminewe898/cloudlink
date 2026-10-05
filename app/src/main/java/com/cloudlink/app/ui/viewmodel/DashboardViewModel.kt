package com.cloudlink.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlink.app.data.model.Resource
import com.cloudlink.app.data.model.Server
import com.cloudlink.app.data.network.SshConnectionManager
import com.cloudlink.app.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ServerMonitorStatus {
    LOADING,
    CONNECTING,
    ONLINE,
    ERROR
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sshManager: SshConnectionManager,
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _serverInfo = MutableStateFlow<Resource<Server>>(Resource.Loading)
    val serverInfo: StateFlow<Resource<Server>> = _serverInfo.asStateFlow()

    private val _systemInfo = MutableStateFlow(SystemInfo())
    val systemInfo: StateFlow<SystemInfo> = _systemInfo.asStateFlow()

    private val _cpuUsage = MutableStateFlow<Float?>(null)
    val cpuUsage: StateFlow<Float?> = _cpuUsage.asStateFlow()

    private val _cpuHistory = MutableStateFlow<List<Float>>(emptyList())
    val cpuHistory: StateFlow<List<Float>> = _cpuHistory.asStateFlow()

    private val _ramUsage = MutableStateFlow<Float?>(null)
    val ramUsage: StateFlow<Float?> = _ramUsage.asStateFlow()

    private val _ramHistory = MutableStateFlow<List<Float>>(emptyList())
    val ramHistory: StateFlow<List<Float>> = _ramHistory.asStateFlow()

    private val _uptime = MutableStateFlow("Unknown")
    val uptime: StateFlow<String> = _uptime.asStateFlow()

    private val _loadAvg = MutableStateFlow("Unknown")
    val loadAvg: StateFlow<String> = _loadAvg.asStateFlow()

    private val _storageUsage = MutableStateFlow("Unknown")
    val storageUsage: StateFlow<String> = _storageUsage.asStateFlow()

    private val _pingMs = MutableStateFlow("Unknown")
    val pingMs: StateFlow<String> = _pingMs.asStateFlow()

    private val _capabilities = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val capabilities: StateFlow<Map<String, Boolean>> = _capabilities.asStateFlow()

    private val _monitorStatus = MutableStateFlow(ServerMonitorStatus.LOADING)
    val monitorStatus: StateFlow<ServerMonitorStatus> = _monitorStatus.asStateFlow()

    private val _monitorError = MutableStateFlow<String?>(null)
    val monitorError: StateFlow<String?> = _monitorError.asStateFlow()

    private var monitorJob: Job? = null
    private var currentServerId: Int? = null

    fun loadServer(serverId: Int) {
        currentServerId = serverId
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            _serverInfo.value = Resource.Loading
            _monitorStatus.value = ServerMonitorStatus.LOADING
            _monitorError.value = null
            val server = serverRepository.getServerById(serverId)
            if (server != null) {
                _serverInfo.value = Resource.Success(server)
                monitorServer(server)
            } else {
                _serverInfo.value = Resource.Error("Server not found")
                _monitorStatus.value = ServerMonitorStatus.ERROR
                _monitorError.value = "This saved server no longer exists."
            }
        }
    }

    fun retry() {
        currentServerId?.let(::loadServer)
    }

    fun pauseMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private suspend fun monitorServer(server: Server) {
        _monitorStatus.value = ServerMonitorStatus.CONNECTING
        val startConnect = System.currentTimeMillis()
        val connectResult = sshManager.connect(server)
        if (connectResult.isFailure) {
            _monitorStatus.value = ServerMonitorStatus.ERROR
            _monitorError.value = connectResult.exceptionOrNull()?.message ?: "Could not connect to this server."
            return
        }

        _pingMs.value = "${System.currentTimeMillis() - startConnect}ms"
        _monitorStatus.value = ServerMonitorStatus.ONLINE
        fetchSystemInfo(server.id)
        detectCapabilities(server.id)

        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            val updated = pollStats(server.id)
            if (!updated) {
                _monitorStatus.value = ServerMonitorStatus.ERROR
                _monitorError.value = "Live telemetry stopped. Check the connection and retry."
                return
            }
            delay(3_000)
        }
    }

    private suspend fun fetchSystemInfo(serverId: Int) {
        val command = """
            printf 'CL_HOST='; (cat /proc/sys/kernel/hostname 2>/dev/null || hostname 2>/dev/null) | head -n 1
            printf 'CL_OS='; (sed -n 's/^PRETTY_NAME=//p' /etc/os-release 2>/dev/null || head -n 1 /etc/issue 2>/dev/null) | head -n 1
            printf 'CL_KERNEL='; uname -r 2>/dev/null
            printf 'CL_ARCH='; uname -m 2>/dev/null
            printf 'CL_CPU_MODEL='; (sed -n 's/^model name[[:space:]]*:[[:space:]]*//p' /proc/cpuinfo 2>/dev/null || sed -n 's/^Hardware[[:space:]]*:[[:space:]]*//p' /proc/cpuinfo 2>/dev/null) | head -n 1
            printf 'CL_CORES='; (getconf _NPROCESSORS_ONLN 2>/dev/null || grep -c '^processor' /proc/cpuinfo 2>/dev/null)
        """.trimIndent()
        val result = sshManager.executeCommand(serverId, command)
        if (result.isSuccess) {
            parseSystemInfo(result.getOrNull().orEmpty())?.let { _systemInfo.value = it }
        }
    }

    private suspend fun detectCapabilities(serverId: Int) {
        val tools = listOf("docker", "docker-compose", "podman", "git", "python3", "java", "node", "nginx", "apache2", "caddy", "tailscale", "wg", "kubectl", "pveversion", "fail2ban-client", "ufw", "smbd", "crontab", "systemctl")
        val command = tools.joinToString("; ") { tool ->
            "command -v $tool >/dev/null 2>&1 && printf '%s\\n' '$tool'"
        }
        val result = sshManager.executeCommand(serverId, command)
        if (result.isSuccess) {
            val detected = result.getOrNull().orEmpty().lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            _capabilities.value = tools.associateWith { it in detected }
        }
    }

    private suspend fun pollStats(serverId: Int): Boolean {
        val command = """
            printf 'CL_CPU='; LC_ALL=C top -bn1 2>/dev/null | awk '/Cpu\(s\)|CPU:/ {for(i=1;i<=NF;i++) if (${ '$' }i ~ /id/) {gsub(/[^0-9.]/,"",${ '$' }(i-1)); printf "%.2f", 100-${ '$' }(i-1); exit}}'; printf '\n'
            printf 'CL_RAM='; awk '/MemTotal:/ {t=${ '$' }2} /MemAvailable:/ {a=${ '$' }2} END {if(t>0 && a>=0) printf "%.2f", (t-a)*100/t}' /proc/meminfo 2>/dev/null; printf '\n'
            printf 'CL_UPTIME='; awk '{print ${ '$' }1}' /proc/uptime 2>/dev/null; printf '\n'
            printf 'CL_STORAGE='; LC_ALL=C df -P / 2>/dev/null | awk 'NR==2 {print ${ '$' }5}'; printf '\n'
            printf 'CL_LOAD='; awk '{print ${ '$' }1, ${ '$' }2, ${ '$' }3}' /proc/loadavg 2>/dev/null; printf '\n'
        """.trimIndent()
        val startPing = System.currentTimeMillis()
        val result = sshManager.executeCommand(serverId, command)

        if (result.isSuccess) {
            val ping = System.currentTimeMillis() - startPing
            _pingMs.value = "${ping}ms"
            val sample = parseTelemetry(result.getOrNull().orEmpty())
            if (sample != null) {
                sample.cpuUsage?.let { value ->
                    _cpuUsage.value = value
                    _cpuHistory.value = (_cpuHistory.value + value).takeLast(20)
                }
                sample.ramUsage?.let { value ->
                    _ramUsage.value = value
                    _ramHistory.value = (_ramHistory.value + value).takeLast(20)
                }
                _uptime.value = sample.uptime
                _storageUsage.value = sample.storageUsage
                _loadAvg.value = sample.loadAverage

                return true
            }
        }
        return false
    }

    override fun onCleared() {
        monitorJob?.cancel()
        super.onCleared()
    }
}
