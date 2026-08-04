package com.cloudlink.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlink.app.data.model.ConnectionLog
import com.cloudlink.app.data.model.Server
import com.cloudlink.app.data.network.SshConnectionManager
import com.cloudlink.app.domain.repository.LogRepository
import com.cloudlink.app.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SessionItem(
    val server: Server,
    val lastActivityAt: Long,
    val lastMessage: String
)

data class SessionsUiState(
    val active: List<SessionItem> = emptyList(),
    val recent: List<SessionItem> = emptyList()
)

@HiltViewModel
class SessionsViewModel @Inject constructor(
    serverRepository: ServerRepository,
    logRepository: LogRepository,
    private val sshConnectionManager: SshConnectionManager
) : ViewModel() {

    val uiState: StateFlow<SessionsUiState> = combine(
        sshConnectionManager.activeSessionIds,
        serverRepository.getAllServers(),
        logRepository.getRecentLogs()
    ) { activeIds, servers, logs ->
        buildSessionsUiState(activeIds, servers, logs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = SessionsUiState()
    )

    fun disconnect(serverId: Int) {
        sshConnectionManager.disconnect(serverId)
    }
}

internal fun buildSessionsUiState(
    activeIds: Set<Int>,
    servers: List<Server>,
    logs: List<ConnectionLog>
): SessionsUiState {
    val latestByServer = logs
        .sortedByDescending { it.timestamp }
        .distinctBy { it.serverId }
        .associateBy { it.serverId }

    fun Server.asSessionItem(): SessionItem {
        val log = latestByServer[id]
        return SessionItem(
            server = this,
            lastActivityAt = log?.timestamp ?: 0L,
            lastMessage = log?.message.orEmpty()
        )
    }

    val active = servers
        .filter { it.id in activeIds }
        .map { it.asSessionItem() }
        .sortedWith(compareByDescending<SessionItem> { it.lastActivityAt }.thenBy { it.server.name })

    val recent = servers
        .filter { it.id !in activeIds && it.id in latestByServer }
        .map { it.asSessionItem() }
        .sortedByDescending { it.lastActivityAt }
        .take(12)

    return SessionsUiState(active = active, recent = recent)
}
