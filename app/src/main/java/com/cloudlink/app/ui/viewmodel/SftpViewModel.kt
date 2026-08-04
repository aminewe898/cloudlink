package com.cloudlink.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlink.app.data.model.RemoteFile
import com.cloudlink.app.data.network.SshConnectionManager
import com.cloudlink.app.domain.repository.ServerRepository
import com.jcraft.jsch.ChannelSftp
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class SftpViewModel @Inject constructor(
    private val sshManager: SshConnectionManager,
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _files = MutableStateFlow<List<RemoteFile>>(emptyList())
    val files: StateFlow<List<RemoteFile>> = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var activeServerId: Int = -1
    private var sftpChannel: ChannelSftp? = null
    private var directoryJob: Job? = null
    private val operationMutex = Mutex()

    fun initServer(serverId: Int) {
        if (serverId < 0) return
        if (activeServerId != serverId) {
            directoryJob?.cancel()
            sftpChannel?.runCatching { disconnect() }
            sftpChannel = null
            activeServerId = serverId
            _currentPath.value = "/"
        }

        viewModelScope.launch(Dispatchers.IO) {
            operationMutex.withLock {
            val server = serverRepository.getServerById(serverId)
            if (server == null) {
                _errorMessage.value = "Server not found."
                return@launch
            }
            val connection = sshManager.connect(server)
            if (connection.isFailure) {
                _errorMessage.value = "Connection failed: ${connection.exceptionOrNull()?.message}"
                return@launch
            }
            loadDirectory(_currentPath.value)
            }
        }
    }

    fun loadDirectory(path: String) {
        if (activeServerId == -1) return

        directoryJob?.cancel()
        val requestedServerId = activeServerId
        directoryJob = viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                operationMutex.withLock {
                    if (requestedServerId != activeServerId) return@withLock
                    val channel = ensureChannel()
                        ?: error("Failed to open SFTP session. Make sure the server is reachable.")
                    refreshDirectory(channel, path)
                }
            } catch (exception: Exception) {
                _errorMessage.value = "SFTP error: ${exception.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFile(fileName: String, isDir: Boolean) {
        deleteFiles(listOf(RemoteFile(fileName, isDir, "", "", "")))
    }

    fun deleteFiles(files: Collection<RemoteFile>) {
        if (files.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                operationMutex.withLock {
                    val channel = ensureChannel() ?: error("SFTP session is unavailable.")
                    val current = _currentPath.value
                    val failures = mutableListOf<String>()
                    files.forEach { file ->
                        runCatching {
                            val path = joinRemotePath(current, file.name)
                            if (file.isDirectory && !file.isSymbolicLink) channel.rmdir(path) else channel.rm(path)
                        }.onFailure { failures += file.name }
                    }
                    refreshDirectory(channel, current)
                    if (failures.isNotEmpty()) error("Could not delete: ${failures.joinToString()}")
                }
            } catch (exception: Exception) {
                _errorMessage.value = "Failed to delete: ${exception.message}"
            }
        }
    }

    fun renameFile(oldName: String, newName: String) {
        if (!isValidRemoteName(newName)) {
            _errorMessage.value = "Invalid file name."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                operationMutex.withLock {
                    val channel = ensureChannel() ?: error("SFTP session is unavailable.")
                    val current = _currentPath.value
                    channel.rename(joinRemotePath(current, oldName), joinRemotePath(current, newName))
                    refreshDirectory(channel, current)
                }
            } catch (exception: Exception) {
                _errorMessage.value = "Failed to rename: ${exception.message}"
            }
        }
    }

    fun createFolder(folderName: String) {
        createRemoteEntry(folderName, isDirectory = true)
    }

    fun createFile(fileName: String) {
        createRemoteEntry(fileName, isDirectory = false)
    }

    private fun createRemoteEntry(name: String, isDirectory: Boolean) {
        if (!isValidRemoteName(name)) {
            _errorMessage.value = "Invalid name. Do not use slashes, null characters, '.' or '..'."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                operationMutex.withLock {
                    val channel = ensureChannel() ?: error("SFTP session is unavailable.")
                    val current = _currentPath.value
                    val path = joinRemotePath(current, name)
                    if (isDirectory) {
                        channel.mkdir(path)
                    } else {
                        channel.put(ByteArray(0).inputStream(), path)
                    }
                    refreshDirectory(channel, current)
                }
            } catch (exception: Exception) {
                _errorMessage.value = "Failed to create ${if (isDirectory) "folder" else "file"}: ${exception.message}"
            }
        }
    }

    fun chmod(fileName: String, permissions: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                operationMutex.withLock {
                    val channel = ensureChannel() ?: error("SFTP session is unavailable.")
                    val current = _currentPath.value
                    channel.chmod(permissions, joinRemotePath(current, fileName))
                    refreshDirectory(channel, current)
                }
            } catch (exception: Exception) {
                _errorMessage.value = "Failed to change permissions: ${exception.message}"
            }
        }
    }

    fun navigateUp() {
        val path = _currentPath.value
        if (path != "/") {
            loadDirectory(path.substringBeforeLast("/", "").ifEmpty { "/" })
        }
    }

    suspend fun readFileContent(path: String): String? = withContext(Dispatchers.IO) {
        try {
            operationMutex.withLock {
                val channel = ensureChannel() ?: error("SFTP session is unavailable.")
                val attrs = channel.lstat(path)
                require(!attrs.isDir) { "Folders cannot be opened in the text editor." }
                require(!attrs.isLink) { "Symbolic links cannot be edited safely in place." }
                val declaredSize = attrs.size
                require(declaredSize <= MAX_EDITABLE_FILE_BYTES) {
                    "File is too large to edit safely (${formatFileSize(declaredSize)}; limit ${formatFileSize(MAX_EDITABLE_FILE_BYTES)})."
                }

                val bytes = channel.get(path).use { input ->
                    val output = ByteArrayOutputStream(declaredSize.coerceAtMost(MAX_EDITABLE_FILE_BYTES).toInt())
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= MAX_EDITABLE_FILE_BYTES) { "File grew beyond the editor size limit." }
                        output.write(buffer, 0, read)
                    }
                    output.toByteArray()
                }
                require(bytes.none { it == 0.toByte() }) { "Binary files cannot be opened in the text editor." }
                bytes.toString(Charsets.UTF_8)
            }
        } catch (exception: Exception) {
            _errorMessage.value = exception.message ?: "Failed to read file."
            null
        }
    }

    suspend fun saveFileContent(path: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val encoded = content.toByteArray(Charsets.UTF_8)
            require(encoded.size <= MAX_EDITABLE_FILE_BYTES) { "Edited file exceeds the size limit." }
            operationMutex.withLock {
                val channel = ensureChannel() ?: error("SFTP session is unavailable.")
                val fileName = path.substringAfterLast('/').ifBlank { "file" }
                val parent = path.substringBeforeLast('/', "/").ifBlank { "/" }
                val temporaryPath = joinRemotePath(parent, ".$fileName.cloudlink-${System.nanoTime()}.tmp")
                val originalMode = runCatching { channel.lstat(path).permissions and 0xFFF }.getOrNull()
                try {
                    channel.put(encoded.inputStream(), temporaryPath)
                    originalMode?.let { channel.chmod(it, temporaryPath) }
                    channel.rename(temporaryPath, path)
                } finally {
                    runCatching { channel.rm(temporaryPath) }
                }
            }
            true
        } catch (exception: Exception) {
            _errorMessage.value = exception.message ?: "Failed to save file."
            false
        }
    }

    private suspend fun ensureChannel(): ChannelSftp? {
        sftpChannel?.takeIf { it.isConnected }?.let { return it }
        val server = serverRepository.getServerById(activeServerId) ?: return null
        val connection = sshManager.connect(server)
        if (connection.isFailure) return null
        return sshManager.getSftpChannel(activeServerId)?.also { sftpChannel = it }
    }

    private fun refreshDirectory(channel: ChannelSftp, path: String) {
        channel.cd(path)
        val currentDir = channel.pwd()
        _currentPath.value = currentDir

        @Suppress("UNCHECKED_CAST")
        val entries = channel.ls(currentDir) as java.util.Vector<ChannelSftp.LsEntry>
        _files.value = entries.mapNotNull { entry ->
            if (entry.filename == "." || entry.filename == "..") return@mapNotNull null
            val attrs = entry.attrs
            RemoteFile(
                name = entry.filename,
                isDirectory = attrs.isDir,
                size = if (attrs.isDir) "--" else formatFileSize(attrs.size),
                permissions = attrs.permissionsString,
                modifiedDate = attrs.mtimeString,
                isSymbolicLink = attrs.isLink,
                rawSize = attrs.size
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    override fun onCleared() {
        sftpChannel?.runCatching { disconnect() }
        directoryJob?.cancel()
        sftpChannel = null
        super.onCleared()
    }

    private companion object {
        const val MAX_EDITABLE_FILE_BYTES = 2L * 1024L * 1024L

        fun isValidRemoteName(name: String): Boolean =
            name.isNotBlank() && name != "." && name != ".." &&
                '/' !in name && '\\' !in name && '\u0000' !in name

        fun joinRemotePath(parent: String, name: String): String =
            if (parent == "/") "/$name" else "${parent.trimEnd('/')}/$name"

        fun formatFileSize(bytes: Long): String = when {
            bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
