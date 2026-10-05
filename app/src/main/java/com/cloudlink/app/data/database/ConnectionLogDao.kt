package com.cloudlink.app.data.database

import androidx.room.*
import com.cloudlink.app.data.model.ConnectionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionLogDao {
    @Query("SELECT * FROM logs WHERE serverId = :serverId ORDER BY timestamp DESC LIMIT 500")
    fun getLogsByServer(serverId: Int): Flow<List<ConnectionLog>>

    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<ConnectionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ConnectionLog)

    @Delete
    suspend fun deleteLog(log: ConnectionLog)

    @Query("DELETE FROM logs WHERE serverId = :serverId")
    suspend fun clearLogsByServer(serverId: Int)

    @Query("DELETE FROM logs")
    suspend fun clearAllLogs()
}
