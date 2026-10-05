package com.cloudlink.app.ui.viewmodel

import com.cloudlink.app.data.model.ConnectionLog
import com.cloudlink.app.data.model.Server
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionsViewModelTest {

    @Test
    fun `active sessions are separated from recent history`() {
        val alpha = server(id = 1, name = "Alpha")
        val beta = server(id = 2, name = "Beta")
        val state = buildSessionsUiState(
            activeIds = setOf(alpha.id),
            servers = listOf(alpha, beta),
            logs = listOf(
                ConnectionLog(serverId = beta.id, timestamp = 300, message = "Disconnected."),
                ConnectionLog(serverId = alpha.id, timestamp = 200, message = "Connection established.")
            )
        )

        assertEquals(listOf(alpha.id), state.active.map { it.server.id })
        assertEquals(listOf(beta.id), state.recent.map { it.server.id })
    }

    @Test
    fun `recent sessions use newest log and ignore deleted servers`() {
        val alpha = server(id = 1, name = "Alpha")
        val state = buildSessionsUiState(
            activeIds = emptySet(),
            servers = listOf(alpha),
            logs = listOf(
                ConnectionLog(serverId = 99, timestamp = 500, message = "Deleted"),
                ConnectionLog(serverId = alpha.id, timestamp = 400, message = "Newest"),
                ConnectionLog(serverId = alpha.id, timestamp = 100, message = "Older")
            )
        )

        assertEquals(1, state.recent.size)
        assertEquals(400L, state.recent.single().lastActivityAt)
        assertEquals("Newest", state.recent.single().lastMessage)
        assertTrue(state.active.isEmpty())
    }

    private fun server(id: Int, name: String) = Server(
        id = id,
        name = name,
        host = "$name.local",
        username = "admin"
    )
}
