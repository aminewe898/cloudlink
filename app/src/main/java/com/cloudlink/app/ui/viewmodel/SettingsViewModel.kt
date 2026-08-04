package com.cloudlink.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlink.app.data.model.ConnectionLog
import com.cloudlink.app.data.model.Server
import com.cloudlink.app.domain.repository.LogRepository
import com.cloudlink.app.domain.repository.ServerRepository
import com.cloudlink.app.data.network.SshConnectionManager
import com.cloudlink.app.ui.theme.AppThemeType
import com.cloudlink.app.ui.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager,
    private val serverRepository: ServerRepository,
    private val logRepository: LogRepository,
    private val sshConnectionManager: SshConnectionManager
) : ViewModel() {

    private val _knownHosts = MutableStateFlow<List<SshConnectionManager.KnownHostEntry>>(emptyList())
    val knownHosts = _knownHosts.asStateFlow()

    init {
        refreshKnownHosts()
    }

    val currentTheme: StateFlow<AppThemeType> = themeManager.currentTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppThemeType.DARK
    )

    val secureScreen: StateFlow<Boolean> = themeManager.secureScreen.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    fun setTheme(theme: AppThemeType) {
        viewModelScope.launch { themeManager.setTheme(theme) }
    }

    fun setSecureScreen(enabled: Boolean) {
        viewModelScope.launch { themeManager.setSecureScreen(enabled) }
    }

    fun resetPreferences() {
        viewModelScope.launch {
            themeManager.setTheme(AppThemeType.DARK)
            themeManager.setSecureScreen(true)
        }
    }

    fun clearConnectionHistory() {
        viewModelScope.launch(Dispatchers.IO) { logRepository.clearAllLogs() }
    }

    fun refreshKnownHosts() {
        _knownHosts.value = sshConnectionManager.getKnownHosts()
    }

    fun removeKnownHost(entry: SshConnectionManager.KnownHostEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            sshConnectionManager.removeKnownHost(entry)
            _knownHosts.value = sshConnectionManager.getKnownHosts()
        }
    }

    suspend fun createConnectionHistoryExport(): String? = withContext(Dispatchers.IO) {
        val logs = logRepository.getRecentLogs(limit = 5_000).first()
        if (logs.isEmpty()) return@withContext null
        val servers = serverRepository.getAllServers().first()
        formatConnectionHistory(servers, logs)
    }
}

internal fun formatConnectionHistory(
    servers: List<Server>,
    logs: List<ConnectionLog>
): String {
    val serverNames = servers.associate { it.id to it.name }
    val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
    return buildString {
        appendLine("CloudLink connection history")
        appendLine("Exported ${formatter.format(Date())}")
        appendLine()
        logs.sortedByDescending { it.timestamp }.forEach { log ->
            val serverName = serverNames[log.serverId] ?: "Server #${log.serverId}"
            appendLine("[${formatter.format(Date(log.timestamp))}] [$serverName] [${log.type}] ${log.message}")
        }
    }
}
