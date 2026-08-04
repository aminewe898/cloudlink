package com.cloudlink.app.data.network

import android.content.Context
import android.util.Base64
import com.cloudlink.app.data.model.AuthType
import com.cloudlink.app.data.model.Server
import com.cloudlink.app.data.security.CredentialManager
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class SshConnectionManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val credentialManager: CredentialManager,
    private val hostKeyPromptCoordinator: HostKeyPromptCoordinator
) {
    data class KnownHostEntry(
        val host: String,
        val algorithm: String,
        val fingerprint: String,
        internal val encodedKey: String
    ) {
        val id: String get() = "$host|$algorithm|$fingerprint"
    }

    private val jsch = JSch()
    private val activeSessions = ConcurrentHashMap<Int, Session>()
    private val connectionLocks = ConcurrentHashMap<Int, Mutex>()
    private val connectionEpochs = ConcurrentHashMap<Int, AtomicLong>()
    private val _activeSessionIds = MutableStateFlow<Set<Int>>(emptySet())
    val activeSessionIds: StateFlow<Set<Int>> = _activeSessionIds.asStateFlow()
    private val _connectionStates = MutableStateFlow<Map<Int, SshConnectionState>>(emptyMap())
    val connectionStates: StateFlow<Map<Int, SshConnectionState>> = _connectionStates.asStateFlow()

    init {
        val knownHostsFile = File(context.filesDir, "known_hosts")
        if (!knownHostsFile.exists()) knownHostsFile.createNewFile()
        jsch.setKnownHosts(knownHostsFile.absolutePath)
    }

    /**
     * Returns an existing live session or establishes exactly one new session per server.
     * New host keys use TOFU; changed or mismatched keys are always rejected.
     */
    suspend fun connect(server: Server): Result<Unit> {
        if (server.host.isBlank()) {
            return Result.failure(SshConnectionException(SshFailureKind.INVALID_HOST, "Enter a valid server host."))
        }
        if (server.port !in 1..65535) {
            return Result.failure(SshConnectionException(SshFailureKind.INVALID_PORT, "Enter a server port from 1 to 65535."))
        }
        val epoch = connectionEpochs.computeIfAbsent(server.id) { AtomicLong() }.get()
        return withContext(Dispatchers.IO) {
        val lock = connectionLocks.computeIfAbsent(server.id) { Mutex() }
        lock.withLock {
            if (connectionEpochs.getValue(server.id).get() != epoch) {
                return@withLock Result.failure(CancellationException("Connection was cancelled"))
            }
            val existing = activeSessions[server.id]
            if (existing?.isConnected == true) {
                updateState(server.id, SshConnectionPhase.CONNECTED)
                publishActiveSessions()
                return@withLock Result.success(Unit)
            }

            updateState(server.id, SshConnectionPhase.CONNECTING, "Opening SSH transport")
            activeSessions.remove(server.id)?.runCatching { disconnect() }
            publishActiveSessions()

            var newSession: Session? = null
            try {
                val session = jsch.getSession(server.username, server.host, server.port)
                newSession = session

                when (server.authType) {
                    AuthType.PASSWORD -> {
                        val password = credentialManager.getPassword(server.id)
                        require(!password.isNullOrEmpty()) { "Password not found" }
                        session.setPassword(password)
                    }

                    AuthType.KEY -> {
                        val privateKey = credentialManager.getPrivateKey(server.id)
                        require(!privateKey.isNullOrEmpty()) { "Private key not found" }
                        val identityName = "key_${server.id}"
                        removeIdentity(identityName)
                        synchronized(jsch) {
                            jsch.addIdentity(identityName, privateKey.toByteArray(), null, null)
                        }
                    }
                }

                updateState(server.id, SshConnectionPhase.AUTHENTICATING, "Authenticating")
                session.setConfig("StrictHostKeyChecking", "ask")
                session.userInfo = object : UserInfo {
                    override fun getPassphrase(): String? = null
                    override fun getPassword(): String? = null
                    override fun promptPassword(message: String?): Boolean = false
                    override fun promptPassphrase(message: String?): Boolean = false

                    override fun promptYesNo(message: String?): Boolean {
                        val normalized = message.orEmpty().lowercase()
                        val indicatesMismatch = normalized.contains("identification has changed") ||
                            normalized.contains("host key has changed") ||
                            normalized.contains("key mismatch")
                        if (indicatesMismatch) return false
                        updateState(server.id, SshConnectionPhase.VERIFYING_HOST, "Waiting for host-key confirmation")
                        return runBlocking {
                            withTimeoutOrNull(HOST_KEY_PROMPT_TIMEOUT_MS) {
                                hostKeyPromptCoordinator.requestConfirmation(message.orEmpty())
                            } ?: false
                        }
                    }

                    override fun showMessage(message: String?) = Unit
                }

                session.connect(CONNECTION_TIMEOUT_MS)
                currentCoroutineContext().ensureActive()
                if (connectionEpochs.getValue(server.id).get() != epoch) {
                    session.disconnect()
                    return@withLock Result.failure(CancellationException("Connection was cancelled"))
                }
                activeSessions[server.id] = session
                updateState(server.id, SshConnectionPhase.CONNECTED)
                publishActiveSessions()
                Result.success(Unit)
            } catch (exception: CancellationException) {
                newSession?.runCatching { disconnect() }
                publishActiveSessions()
                throw exception
            } catch (exception: Exception) {
                newSession?.runCatching { disconnect() }
                publishActiveSessions()
                if (server.authType == AuthType.KEY) {
                    removeIdentity("key_${server.id}")
                }
                val mapped = mapSshException(exception)
                updateState(server.id, SshConnectionPhase.FAILED, mapped.message, mapped.kind)
                Result.failure(mapped)
            }
        }
        }
    }

    fun disconnect(serverId: Int) {
        connectionEpochs.computeIfAbsent(serverId) { AtomicLong() }.incrementAndGet()
        updateState(serverId, SshConnectionPhase.DISCONNECTING)
        activeSessions.remove(serverId)?.runCatching { disconnect() }
        removeIdentity("key_$serverId")
        updateState(serverId, SshConnectionPhase.DISCONNECTED)
        publishActiveSessions()
    }

    fun disconnectAll() {
        activeSessions.keys.toList().forEach(::disconnect)
    }

    private fun publishActiveSessions() {
        _activeSessionIds.value = activeSessions
            .filterValues { it.isConnected }
            .keys
            .toSet()
    }

    fun getKnownHosts(): List<KnownHostEntry> = synchronized(jsch) {
        jsch.hostKeyRepository.hostKey
            .map { hostKey ->
                KnownHostEntry(
                    host = hostKey.host,
                    algorithm = hostKey.type,
                    fingerprint = hostKey.getFingerPrint(jsch),
                    encodedKey = hostKey.key
                )
            }
            .sortedWith(compareBy({ it.host }, { it.algorithm }))
    }

    fun removeKnownHost(entry: KnownHostEntry): Result<Unit> = runCatching {
        synchronized(jsch) {
            val key = Base64.decode(entry.encodedKey, Base64.DEFAULT)
            jsch.hostKeyRepository.remove(entry.host, entry.algorithm, key)
        }
    }

    fun validatePrivateKey(privateKey: String): Result<Unit> = runCatching {
        require(privateKey.isNotBlank()) { "The private key is empty." }
        val validator = JSch()
        validator.addIdentity("validation", privateKey.toByteArray(), null, null)
        require(validator.identityRepository.identities.none { it.isEncrypted }) {
            "Encrypted private keys are not supported because CloudLink does not store passphrases."
        }
        validator.removeAllIdentity()
    }.recoverCatching { exception ->
        val mapped = mapSshException(exception)
        throw if (mapped.kind == SshFailureKind.UNSUPPORTED_KEY) {
            mapped
        } else {
            SshConnectionException(
                SshFailureKind.UNSUPPORTED_KEY,
                "The private key is malformed, encrypted, or unsupported.",
                exception
            )
        }
    }

    private fun removeIdentity(identityName: String) {
        runCatching {
            synchronized(jsch) {
                jsch.identityRepository.identities
                    .filter { it.name == identityName }
                    .forEach { jsch.removeIdentity(it) }
            }
        }
    }

    private fun updateState(
        serverId: Int,
        phase: SshConnectionPhase,
        message: String? = null,
        failureKind: SshFailureKind? = null
    ) {
        _connectionStates.update { states ->
            states + (serverId to SshConnectionState(phase, message, failureKind))
        }
    }

    suspend fun executeCommand(serverId: Int, command: String): Result<String> =
        withContext(Dispatchers.IO) {
            val session = activeSessions[serverId]
                ?: return@withContext Result.failure(IllegalStateException("No active session"))
            if (!session.isConnected) {
                return@withContext Result.failure(IllegalStateException("Session disconnected"))
            }

            var channel: ChannelExec? = null
            try {
                channel = session.openChannel("exec") as ChannelExec
                channel.setCommand(command)
                val outputStream = channel.inputStream
                val errorStream = channel.errStream
                channel.connect(CONNECTION_TIMEOUT_MS)

                val (output, error) = withTimeout(COMMAND_TIMEOUT_MS) {
                    coroutineScope {
                        val stdout = async(Dispatchers.IO) {
                            outputStream.bufferedReader().use { it.readText() }
                        }
                        val stderr = async(Dispatchers.IO) {
                            errorStream.bufferedReader().use { it.readText() }
                        }
                        stdout.await() to stderr.await()
                    }
                }

                if (error.isNotBlank() && output.isBlank()) {
                    Result.failure(IllegalStateException(error.trim()))
                } else {
                    Result.success(output)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(mapSshException(exception))
            } finally {
                channel?.runCatching { disconnect() }
            }
        }

    suspend fun getSftpChannel(serverId: Int): ChannelSftp? = withContext(Dispatchers.IO) {
        try {
            val session = activeSessions[serverId] ?: return@withContext null
            if (!session.isConnected) return@withContext null
            (session.openChannel("sftp") as ChannelSftp).apply {
                connect(CONNECTION_TIMEOUT_MS)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getShellChannel(serverId: Int): ChannelShell? = withContext(Dispatchers.IO) {
        try {
            val session = activeSessions[serverId] ?: return@withContext null
            if (!session.isConnected) return@withContext null
            session.openChannel("shell") as ChannelShell
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val CONNECTION_TIMEOUT_MS = 10_000
        const val COMMAND_TIMEOUT_MS = 30_000L
        const val HOST_KEY_PROMPT_TIMEOUT_MS = 60_000L
    }
}
