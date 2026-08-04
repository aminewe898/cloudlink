package com.cloudlink.app.data.network

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HostKeyPrompt internal constructor(
    val id: String,
    val message: String,
    internal val response: CompletableDeferred<Boolean>
)

@Singleton
class HostKeyPromptCoordinator @Inject constructor() {
    private val promptMutex = Mutex()
    private val _pendingPrompt = MutableStateFlow<HostKeyPrompt?>(null)
    val pendingPrompt: StateFlow<HostKeyPrompt?> = _pendingPrompt.asStateFlow()

    suspend fun requestConfirmation(message: String): Boolean = promptMutex.withLock {
        val prompt = HostKeyPrompt(
            id = UUID.randomUUID().toString(),
            message = message,
            response = CompletableDeferred()
        )
        _pendingPrompt.value = prompt
        try {
            prompt.response.await()
        } finally {
            if (_pendingPrompt.value?.id == prompt.id) _pendingPrompt.value = null
        }
    }

    fun respond(promptId: String, accepted: Boolean) {
        _pendingPrompt.value
            ?.takeIf { it.id == promptId }
            ?.response
            ?.complete(accepted)
    }
}
