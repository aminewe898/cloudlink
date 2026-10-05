package com.cloudlink.app.ui.viewmodel

import com.cloudlink.app.data.model.ConnectionLog
import com.cloudlink.app.data.model.LogType
import com.cloudlink.app.data.model.Server
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {

    @Test
    fun `history export names servers and orders newest first`() {
        val server = Server(id = 7, name = "Lab node", host = "lab.local", username = "admin")
        val export = formatConnectionHistory(
            servers = listOf(server),
            logs = listOf(
                ConnectionLog(serverId = 7, timestamp = 100, message = "Older", type = LogType.SYSTEM),
                ConnectionLog(serverId = 7, timestamp = 200, message = "Newest", type = LogType.ERROR)
            )
        )

        assertTrue(export.contains("[Lab node]"))
        assertTrue(export.contains("[ERROR] Newest"))
        assertTrue(export.indexOf("Newest") < export.indexOf("Older"))
    }
}
