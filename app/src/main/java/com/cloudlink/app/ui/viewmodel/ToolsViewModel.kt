package com.cloudlink.app.ui.viewmodel

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ToolsViewModel @Inject constructor() : ViewModel() {

    data class GeneratedSshKey(
        val publicKey: String,
        val privateKey: String,
        val fingerprint: String
    )

    private val _toolOutput = MutableStateFlow("")
    val toolOutput: StateFlow<String> = _toolOutput.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _lastRunSucceeded = MutableStateFlow<Boolean?>(null)
    val lastRunSucceeded: StateFlow<Boolean?> = _lastRunSucceeded.asStateFlow()

    private val _generatedSshKey = MutableStateFlow<GeneratedSshKey?>(null)
    val generatedSshKey: StateFlow<GeneratedSshKey?> = _generatedSshKey.asStateFlow()

    private var executionJob: Job? = null
    private var activeProcess: Process? = null

    fun runPing(host: String) {
        val sanitizedHost = host.trim()
        if (!isValidHostOrIp(sanitizedHost)) {
            showValidationError("Enter a valid host name or IP address.")
            return
        }
        launchTool("Pinging $sanitizedHost…") {
            val executable = if (':' in sanitizedHost) "ping6" else "ping"
            val process = ProcessBuilder(executable, "-c", "4", "-W", "3", sanitizedHost)
                .redirectErrorStream(true)
                .start()
            activeProcess = process
            try {
                coroutineScope {
                    val outputReader = async(Dispatchers.IO) {
                        process.inputStream.bufferedReader().use { it.readText().trim() }
                    }
                    val exitCode = awaitProcess(process, PING_TIMEOUT_MILLIS)
                    val output = outputReader.await()
                    if (exitCode != 0) {
                        error(output.ifBlank { "The host did not respond." })
                    }
                    output.ifBlank { "Ping completed successfully." }
                }
            } finally {
                process.destroy()
            }
        }
    }

    fun runWakeOnLan(macAddress: String, ipAddress: String = DEFAULT_BROADCAST_ADDRESS) {
        val mac = runCatching { parseMacAddress(macAddress) }.getOrElse {
            showValidationError(it.message ?: "Enter a valid MAC address.")
            return
        }
        val destination = ipAddress.trim().ifBlank { DEFAULT_BROADCAST_ADDRESS }
        launchTool("Sending a magic packet to ${normalizeMacAddress(mac)}…") {
            val magicPacket = ByteArray(6 + 16 * mac.size)
            for (index in 0 until 6) magicPacket[index] = 0xFF.toByte()
            var offset = 6
            while (offset < magicPacket.size) {
                System.arraycopy(mac, 0, magicPacket, offset, mac.size)
                offset += mac.size
            }

            val address = InetAddress.getByName(destination)
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.send(DatagramPacket(magicPacket, magicPacket.size, address, WAKE_ON_LAN_PORT))
            }
            "Magic packet sent to ${normalizeMacAddress(mac)} via $destination:$WAKE_ON_LAN_PORT."
        }
    }

    fun generateSshKey() {
        launchTool("Generating a 4096-bit RSA key pair…") {
            val keyPair = KeyPair.genKeyPair(JSch(), KeyPair.RSA, RSA_KEY_SIZE)
            val privateStream = ByteArrayOutputStream()
            val publicStream = ByteArrayOutputStream()
            try {
                keyPair.writePrivateKey(privateStream)
                keyPair.writePublicKey(publicStream, "cloudlink-generated-key")
                val publicKey = publicStream.toString(Charsets.UTF_8.name()).trim()
                val privateKey = privateStream.toString(Charsets.UTF_8.name()).trim()
                val publicBlob = publicKey.split(Regex("\\s+")).getOrNull(1)
                    ?.let { Base64.decode(it, Base64.DEFAULT) }
                    ?: error("Generated public key could not be encoded.")
                val digest = MessageDigest.getInstance("SHA-256").digest(publicBlob)
                val fingerprint = "SHA256:" + Base64.encodeToString(
                    digest,
                    Base64.NO_WRAP or Base64.NO_PADDING
                )
                _generatedSshKey.value = GeneratedSshKey(publicKey, privateKey, fingerprint)
                buildString {
                    appendLine("FINGERPRINT")
                    appendLine(fingerprint)
                    appendLine()
                    appendLine("PUBLIC KEY")
                    appendLine(publicKey)
                    appendLine()
                    append("The private key is hidden. Use the explicit reveal action only when you are ready to store it securely. CloudLink has not saved this key pair.")
                }
            } finally {
                keyPair.dispose()
                privateStream.close()
                publicStream.close()
            }
        }
    }

    fun clearOutput() {
        if (_isExecuting.value) return
        _toolOutput.value = ""
        _lastRunSucceeded.value = null
        _generatedSshKey.value = null
    }

    fun cancelExecution() {
        activeProcess?.destroy()
        activeProcess = null
        executionJob?.cancel()
    }

    private fun launchTool(startMessage: String, block: suspend () -> String) {
        cancelExecution()
        executionJob = viewModelScope.launch(Dispatchers.IO) {
            _isExecuting.value = true
            _lastRunSucceeded.value = null
            _toolOutput.value = startMessage
            try {
                _toolOutput.value = block()
                _lastRunSucceeded.value = true
            } catch (_: CancellationException) {
                _toolOutput.value = "Operation cancelled."
                _lastRunSucceeded.value = false
            } catch (exception: Exception) {
                _toolOutput.value = "Error: ${exception.message ?: "The operation failed."}"
                _lastRunSucceeded.value = false
            } finally {
                activeProcess = null
                _isExecuting.value = false
            }
        }
    }

    private fun showValidationError(message: String) {
        _toolOutput.value = "Error: $message"
        _lastRunSucceeded.value = false
    }

    override fun onCleared() {
        cancelExecution()
        super.onCleared()
    }

    private companion object {
        const val PING_TIMEOUT_MILLIS = 20_000L
        const val DEFAULT_BROADCAST_ADDRESS = "255.255.255.255"
        const val WAKE_ON_LAN_PORT = 9
        const val RSA_KEY_SIZE = 4096
    }
}

internal fun isValidHostOrIp(value: String): Boolean {
    if (value.isBlank() || value.length > 253 || ".." in value) return false
    if (':' in value) {
        return runCatching { InetAddress.getByName(value).address.size == 16 }.getOrDefault(false)
    }
    return Regex("^[a-zA-Z0-9](?:[a-zA-Z0-9.-]*[a-zA-Z0-9])?$").matches(value)
}

private suspend fun awaitProcess(process: Process, timeoutMillis: Long): Int {
    val deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L
    while (true) {
        try {
            return process.exitValue()
        } catch (_: IllegalThreadStateException) {
            if (System.nanoTime() >= deadlineNanos) {
                process.destroy()
                error("Ping timed out after ${timeoutMillis / 1_000} seconds.")
            }
            delay(100)
        }
    }
}

internal fun parseMacAddress(value: String): ByteArray {
    val compact = value.trim().replace(":", "").replace("-", "")
    require(compact.length == 12 && compact.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
        "Enter a MAC address such as 00:11:22:33:44:55."
    }
    return ByteArray(6) { index -> compact.substring(index * 2, index * 2 + 2).toInt(16).toByte() }
}

private fun normalizeMacAddress(bytes: ByteArray): String =
    bytes.joinToString(":") { byte -> "%02X".format(byte.toInt() and 0xFF) }
