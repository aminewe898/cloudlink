package com.cloudlink.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlink.app.data.model.AuthType
import com.cloudlink.app.data.model.Server
import com.cloudlink.app.data.model.ServerFolder
import com.cloudlink.app.data.network.SshConnectionManager
import com.cloudlink.app.data.security.CredentialManager
import com.cloudlink.app.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ServerListViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val credentialManager: CredentialManager,
    private val sshConnectionManager: SshConnectionManager
) : ViewModel() {

    private val _operationErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val operationErrors = _operationErrors.asSharedFlow()

    private val _currentFolder = MutableStateFlow(ServerFolder.ALL)
    val currentFolder: StateFlow<ServerFolder> = _currentFolder.asStateFlow()

    val servers: StateFlow<List<Server>> = _currentFolder
        .flatMapLatest { folder ->
            if (folder == ServerFolder.ALL) {
                serverRepository.getAllServers()
            } else {
                serverRepository.getServersByFolder(folder)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList()
        )

    fun setFolder(folder: ServerFolder) {
        _currentFolder.value = folder
    }

    fun deleteServer(server: Server) {
        viewModelScope.launch {
            runCatching {
                sshConnectionManager.disconnect(server.id)
                serverRepository.deleteServer(server)
                credentialManager.deleteCredentials(server.id)
            }.onFailure { _operationErrors.tryEmit(it.message ?: "The server could not be deleted.") }
        }
    }

    fun addServer(server: Server, credentialText: String) {
        viewModelScope.launch {
            runCatching {
                if (server.authType == AuthType.KEY) {
                    sshConnectionManager.validatePrivateKey(credentialText).getOrThrow()
                }
                val id = serverRepository.insertServer(server)
                try {
                    credentialManager.replaceCredential(id, server.authType, credentialText)
                } catch (exception: Exception) {
                    serverRepository.getServerById(id)?.let { serverRepository.deleteServer(it) }
                    throw exception
                }
            }.onFailure {
                _operationErrors.tryEmit(it.message ?: "The server could not be saved.")
            }
        }
    }

    fun updateServer(server: Server, credentialText: String?) {
        viewModelScope.launch {
            runCatching {
                val previous = serverRepository.getServerById(server.id)
                    ?: error("This server no longer exists.")
                val authChanged = previous.authType != server.authType
                if (authChanged) require(!credentialText.isNullOrBlank()) {
                    "Enter a new credential when changing the authentication method."
                }
                if (server.authType == AuthType.KEY && !credentialText.isNullOrBlank()) {
                    sshConnectionManager.validatePrivateKey(credentialText).getOrThrow()
                }
                serverRepository.updateServer(server)
                try {
                    if (!credentialText.isNullOrEmpty()) {
                        credentialManager.replaceCredential(server.id, server.authType, credentialText)
                    }
                } catch (exception: Exception) {
                    serverRepository.updateServer(previous)
                    throw exception
                }
                if (authChanged || !credentialText.isNullOrEmpty()) {
                    sshConnectionManager.disconnect(server.id)
                }
            }.onFailure {
                _operationErrors.tryEmit(it.message ?: "The server could not be updated.")
            }
        }
    }

    fun toggleFavorite(server: Server) {
        viewModelScope.launch {
            val updated = server.copy(favorite = !server.favorite)
            serverRepository.updateServer(updated)
        }
    }
}
