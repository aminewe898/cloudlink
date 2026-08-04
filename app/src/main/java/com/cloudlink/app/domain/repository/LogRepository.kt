package com.cloudlink.app.domain.repository

import com.cloudlink.app.data.model.ConnectionLog
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    fun getLogsForServer(serverId: Int): Flow<List<ConnectionLog>>
    fun getRecentLogs(limit: Int = 100): Flow<List<ConnectionLog>>
    suspend fun addLog(log: ConnectionLog)
    suspend fun clearLogs(serverId: Int)
    suspend fun clearAllLogs()
}
