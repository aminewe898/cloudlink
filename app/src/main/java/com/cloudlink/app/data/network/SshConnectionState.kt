package com.cloudlink.app.data.network

enum class SshConnectionPhase {
    IDLE,
    CONNECTING,
    AUTHENTICATING,
    VERIFYING_HOST,
    CONNECTED,
    DISCONNECTING,
    DISCONNECTED,
    FAILED
}

enum class SshFailureKind {
    INVALID_HOST,
    INVALID_PORT,
    DNS,
    TIMEOUT,
    AUTHENTICATION,
    HOST_KEY,
    UNSUPPORTED_KEY,
    NETWORK,
    SERVER_DISCONNECT,
    UNKNOWN
}

data class SshConnectionState(
    val phase: SshConnectionPhase = SshConnectionPhase.IDLE,
    val message: String? = null,
    val failureKind: SshFailureKind? = null
)

class SshConnectionException(
    val kind: SshFailureKind,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

internal fun mapSshException(exception: Throwable): SshConnectionException {
    if (exception is SshConnectionException) return exception
    val message = exception.message.orEmpty().lowercase()
    val kind = when {
        exception is java.net.UnknownHostException || "unknownhost" in message || "unknown host" in message ->
            SshFailureKind.DNS
        "timeout" in message || "timed out" in message -> SshFailureKind.TIMEOUT
        "auth fail" in message || "authentication" in message || "password" in message ->
            SshFailureKind.AUTHENTICATION
        "hostkey" in message || "host key" in message || "reject hostkey" in message ->
            SshFailureKind.HOST_KEY
        "invalid privatekey" in message || "private key" in message || "invalid key" in message ->
            SshFailureKind.UNSUPPORTED_KEY
        "connection refused" in message || "network is unreachable" in message || "no route" in message ->
            SshFailureKind.NETWORK
        "socket is not established" in message || "session is down" in message || "connection is closed" in message ->
            SshFailureKind.SERVER_DISCONNECT
        else -> SshFailureKind.UNKNOWN
    }
    val userMessage = when (kind) {
        SshFailureKind.INVALID_HOST -> "Enter a valid server host."
        SshFailureKind.INVALID_PORT -> "Enter a server port from 1 to 65535."
        SshFailureKind.DNS -> "The server name could not be resolved."
        SshFailureKind.TIMEOUT -> "The SSH connection timed out. Check the address, port, and network."
        SshFailureKind.AUTHENTICATION -> "Authentication failed. Check the username and saved credential."
        SshFailureKind.HOST_KEY -> "SSH host verification failed. The server key was not trusted."
        SshFailureKind.UNSUPPORTED_KEY -> "The private key is malformed, encrypted, or unsupported."
        SshFailureKind.NETWORK -> "The server could not be reached from this network."
        SshFailureKind.SERVER_DISCONNECT -> "The SSH server closed the connection."
        SshFailureKind.UNKNOWN -> exception.message?.takeIf { it.isNotBlank() }
            ?: "The SSH connection failed."
    }
    return SshConnectionException(kind, userMessage, exception)
}
