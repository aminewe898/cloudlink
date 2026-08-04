package com.cloudlink.app.domain.repository

import com.cloudlink.app.data.model.Server
import com.cloudlink.app.data.model.ServerFolder
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    fun getAllServers(): Flow<List<Server>>
    fun getFavoriteServers(): Flow<List<Server>>
    fun getServersByFolder(folder: ServerFolder): Flow<List<Server>>
    suspend fun getServerById(id: Int): Server?
    suspend fun insertServer(server: Server): Int
    suspend fun updateServer(server: Server)
    suspend fun deleteServer(server: Server)
}
