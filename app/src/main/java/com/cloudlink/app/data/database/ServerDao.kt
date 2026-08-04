package com.cloudlink.app.data.database

import androidx.room.*
import com.cloudlink.app.data.model.Server
import com.cloudlink.app.data.model.ServerFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY name ASC")
    fun getAllServers(): Flow<List<Server>>

    @Query("SELECT * FROM servers WHERE favorite = 1 ORDER BY name ASC")
    fun getFavoriteServers(): Flow<List<Server>>

    @Query("SELECT * FROM servers WHERE folder = :folder ORDER BY name ASC")
    fun getServersByFolder(folder: ServerFolder): Flow<List<Server>>

    @Query("SELECT * FROM servers WHERE id = :id LIMIT 1")
    suspend fun getServerById(id: Int): Server?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: Server): Long

    @Update
    suspend fun updateServer(server: Server)

    @Delete
    suspend fun deleteServer(server: Server)
}
