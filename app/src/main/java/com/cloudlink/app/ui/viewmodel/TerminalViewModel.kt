package com.cloudlink.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlink.app.data.model.ConnectionLog
import com.cloudlink.app.data.model.LogType
import com.cloudlink.app.data.network.SshConnectionManager
import com.cloudlink.app.domain.repository.LogRepository
import com.cloudlink.app.domain.repository.ServerRepository
import com.cloudlink.app.terminal.TerminalBuffer
import com.cloudlink.app.terminal.TerminalSnapshot
import com.cloudlink.app.terminal.Vt100Parser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicInteger

enum class TerminalConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val sshManager: SshConnectionManager,
    private val logRepository: LogRepository,
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _terminalSnapshot = MutableStateFlow<TerminalSnapshot?>(null)
    val terminalSnapshot: StateFlow<TerminalSnapshot?> = _terminalSnapshot.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionStatus = MutableStateFlow(TerminalConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<TerminalConnectionStatus> = _connectionStatus.asStateFlow()

    private val _sessionLabel = MutableStateFlow("SSH terminal")
    val sessionLabel: StateFlow<String> = _sessionLabel.asStateFlow()

    private val _statusDetail = MutableStateFlow("Choose a saved server to begin")
    val statusDetail: StateFlow<String> = _statusDetail.asStateFlow()

    private var currentServerId: Int? = null

    private var shellChannel: com.jcraft.jsch.ChannelShell? = null
    private var shellOutputStream: java.io.OutputStream? = null

    private var readJob: Job? = null
    private var connectJob: Job? = null
    private var shellJob: Job? = null
    private var reconnectJob: Job? = null
    private var writeJob: Job? = null
    private var renderJob: Job? = null

    private val connectionGeneration = AtomicInteger(0)

    private val terminalBuffer = TerminalBuffer(cols = 80, rows = 24)
    private val parser = Vt100Parser(terminalBuffer) { response -> sendString(response) }

    private data class PendingWrite(val generation: Int, val serverId: Int, val data: ByteArray)

    private val writeChannel = Channel<PendingWrite>(capacity = 64)
    private val renderChannel = Channel<Unit>(Channel.CONFLATED)

    init {
        // Render loop: up to 60fps (16ms)
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            for (event in renderChannel) {
                _terminalSnapshot.value = terminalBuffer.getSnapshot()
                delay(16)
            }
        }

        // Writer loop
        writeJob = viewModelScope.launch(Dispatchers.IO) {
            for (pending in writeChannel) {
                if (pending.generation != connectionGeneration.get() || pending.serverId != currentServerId) {
                    continue
                }
                try {
                    val output = shellOutputStream ?: error("Remote shell input is unavailable")
                    output.write(pending.data)
                    output.flush()
                } catch (_: CancellationException) {
                    throw CancellationException()
                } catch (exception: Exception) {
                    viewModelScope.launch {
                        handleDisconnect(pending.serverId, pending.generation, exception.message)
                    }
                }
            }
        }

        // Initial snapshot
        _terminalSnapshot.value = terminalBuffer.getSnapshot()
    }

    fun connect(serverId: Int) {
        if (currentServerId == serverId && _isConnected.value) return
        val generation = connectionGeneration.incrementAndGet()
        reconnectJob?.cancel()
        reconnectJob = null
        shellJob?.cancel()
        readJob?.cancel()
        cleanupShell(logDisconnect = currentServerId != null && currentServerId != serverId)
        _connectionStatus.value = TerminalConnectionStatus.CONNECTING
        _statusDetail.value = "Opening a secure session…"

        connectJob?.cancel()
        connectJob = viewModelScope.launch {
            currentServerId = serverId
            val server = serverRepository.getServerById(serverId)
            if (server == null) {
                _connectionStatus.value = TerminalConnectionStatus.ERROR
                _statusDetail.value = "This server is no longer available"
                return@launch
            }
            _sessionLabel.value = server.name
            _statusDetail.value = "${server.username}@${server.host}:${server.port}"

            logRepository.addLog(ConnectionLog(serverId = serverId, message = "Connecting to ${server.name}...", type = LogType.SYSTEM))

            val result = sshManager.connect(server)
            if (generation != connectionGeneration.get() || currentServerId != serverId) return@launch
            if (result.isSuccess) {
                startShell(serverId, generation)
            } else {
                _isConnected.value = false
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _connectionStatus.value = TerminalConnectionStatus.ERROR
                _statusDetail.value = error
                logRepository.addLog(ConnectionLog(serverId = serverId, message = "Connection failed: $error", type = LogType.ERROR))
            }
        }
    }

    private fun startShell(serverId: Int, generation: Int) {
        shellJob?.cancel()
        shellJob = viewModelScope.launch {
            val channel = sshManager.getShellChannel(serverId)
            if (generation != connectionGeneration.get() || currentServerId != serverId) {
                channel?.runCatching { disconnect() }
                return@launch
            }
            if (channel != null) {
                try {
                    channel.setPty(true)
                    channel.setPtyType("xterm-256color")
                    channel.setPtySize(terminalBuffer.cols, terminalBuffer.rows, terminalBuffer.cols * 8, terminalBuffer.rows * 16)
                } catch (exception: Exception) {
                    _statusDetail.value = "PTY negotiation warning: ${exception.message ?: "using server defaults"}"
                }

                try {
                    shellOutputStream = channel.outputStream
                    val inputStreamToRead = channel.inputStream

                    withContext(Dispatchers.IO) {
                        channel.connect(5000)
                    }
                    if (generation != connectionGeneration.get() || currentServerId != serverId) {
                        channel.disconnect()
                        return@launch
                    }
                    shellChannel = channel
                    _isConnected.value = true
                    _connectionStatus.value = TerminalConnectionStatus.CONNECTED
                    logRepository.addLog(ConnectionLog(serverId = serverId, message = "Connection established.", type = LogType.SYSTEM))

                    // Request an initial screen redraw from the terminal
                    renderChannel.trySend(Unit)

                    readJob = viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val inputStream = java.io.InputStreamReader(inputStreamToRead, "UTF-8")
                            val buffer = CharArray(4096)
                            var charsRead = 0
                            while (isActive && inputStream.read(buffer).also { charsRead = it } != -1) {
                                val chunk = String(buffer, 0, charsRead)
                                parser.process(chunk)
                                renderChannel.trySend(Unit)
                            }
                            if (isActive) {
                                viewModelScope.launch { handleDisconnect(serverId, generation, "The remote shell closed") }
                            }
                        } catch (_: CancellationException) {
                            // Expected during a deliberate close or server switch.
                        } catch (exception: Exception) {
                            viewModelScope.launch { handleDisconnect(serverId, generation, exception.message) }
                        }
                    }
                } catch (_: CancellationException) {
                    channel.runCatching { disconnect() }
                } catch (e: Exception) {
                    channel.runCatching { disconnect() }
                    _connectionStatus.value = TerminalConnectionStatus.ERROR
                    _statusDetail.value = e.message ?: "Could not start the remote shell"
                    logRepository.addLog(ConnectionLog(serverId = serverId, message = "Failed to start shell: ${e.message}", type = LogType.ERROR))
                    handleDisconnect(serverId, generation, e.message)
                }
            } else {
                _connectionStatus.value = TerminalConnectionStatus.ERROR
                _statusDetail.value = "The SSH connection did not provide a shell channel"
            }
        }
    }

    fun sendBytes(data: ByteArray) {
        val serverId = currentServerId ?: return
        if (!_isConnected.value) return
        val queued = writeChannel.trySend(PendingWrite(connectionGeneration.get(), serverId, data.copyOf()))
        if (queued.isFailure) {
            _statusDetail.value = "Input paused because the network queue is full"
        }
    }

    fun sendString(data: String) {
        sendBytes(data.toByteArray(Charsets.UTF_8))
    }

    fun sendCursorKey(final: Char) {
        require(final in listOf('A', 'B', 'C', 'D')) { "Unsupported cursor key" }
        sendString(if (terminalBuffer.applicationCursorKeys) "\u001BO$final" else "\u001B[$final")
    }

    fun pasteText(text: String) {
        val normalized = text.replace("\r\n", "\n").replace("\n", "\r")
        val payload = if (terminalBuffer.bracketedPasteMode) {
            "\u001B[200~$normalized\u001B[201~"
        } else {
            normalized
        }
        sendString(payload)
    }

    fun clearTerminal() {
        terminalBuffer.clearHistory()
        renderChannel.trySend(Unit)
    }

    fun updateTerminalSize(
        cols: Int,
        rows: Int,
        widthPx: Int = cols * 8,
        heightPx: Int = rows * 16
    ) {
        if (cols > 0 && rows > 0 && (cols != terminalBuffer.cols || rows != terminalBuffer.rows)) {
            terminalBuffer.resize(cols, rows)
            renderChannel.trySend(Unit)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    shellChannel?.setPtySize(cols, rows, widthPx, heightPx)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun disconnect() {
        val serverId = currentServerId
        connectionGeneration.incrementAndGet()
        connectJob?.cancel()
        shellJob?.cancel()
        reconnectJob?.cancel()
        cleanupShell(logDisconnect = true)
        serverId?.let(sshManager::disconnect)
        _connectionStatus.value = TerminalConnectionStatus.DISCONNECTED
        _statusDetail.value = "Session closed"
    }

    private fun cleanupShell(logDisconnect: Boolean) {
        readJob?.cancel()
        runCatching { shellChannel?.disconnect() }
        runCatching { shellOutputStream?.close() }
        shellChannel = null
        shellOutputStream = null

        while (writeChannel.tryReceive().isSuccess) Unit
        currentServerId?.let {
            _isConnected.value = false
            if (logDisconnect) viewModelScope.launch {
                logRepository.addLog(ConnectionLog(serverId = it, message = "Disconnected.", type = LogType.SYSTEM))
            }
        }
        currentServerId = null
    }

    private fun handleDisconnect(serverId: Int, generation: Int, detail: String?) {
        if (generation != connectionGeneration.get() || currentServerId != serverId) return
        if (reconnectJob?.isActive == true) return
        cleanupShell(logDisconnect = false)
        sshManager.disconnect(serverId)
        _connectionStatus.value = TerminalConnectionStatus.RECONNECTING
        _statusDetail.value = detail?.takeIf { it.isNotBlank() }
            ?.let { "$it · retrying in 3 seconds" }
            ?: "Connection lost · retrying in 3 seconds"
        reconnectJob = viewModelScope.launch {
            logRepository.addLog(ConnectionLog(serverId = serverId, message = "Connection lost. Reconnecting in 3s...", type = LogType.SYSTEM))
            delay(3000)
            if (generation == connectionGeneration.get()) {
                reconnectJob = null
                connect(serverId)
            }
        }
    }

    override fun onCleared() {
        connectJob?.cancel()
        shellJob?.cancel()
        reconnectJob?.cancel()
        disconnect()
        renderJob?.cancel()
        writeJob?.cancel()
        writeChannel.close()
        renderChannel.close()
        super.onCleared()
    }
}
