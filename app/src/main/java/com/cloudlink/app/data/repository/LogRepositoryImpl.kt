package com.cloudlink.app.data.repository

import com.cloudlink.app.data.database.ConnectionLogDao
import com.cloudlink.app.data.model.ConnectionLog
import com.cloudlink.app.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LogRepositoryImpl @Inject constructor(
    private val logDao: ConnectionLogDao
) : LogRepository {
    override fun getLogsForServer(serverId: Int): Flow<List<ConnectionLog>> = logDao.getLogsByServer(serverId)

    override fun getRecentLogs(limit: Int): Flow<List<ConnectionLog>> = logDao.getRecentLogs(limit)

    override suspend fun addLog(log: ConnectionLog) {
        logDao.insertLog(log)
    }

    override suspend fun clearLogs(serverId: Int) {
        logDao.clearLogsByServer(serverId)
    }

    override suspend fun clearAllLogs() {
        logDao.clearAllLogs()
    }
}
