package com.cloudlink.app.data.repository

import com.cloudlink.app.data.database.ServerDao
import com.cloudlink.app.data.model.Server
import com.cloudlink.app.data.model.ServerFolder
import com.cloudlink.app.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ServerRepositoryImpl @Inject constructor(
    private val serverDao: ServerDao
) : ServerRepository {

    override fun getAllServers(): Flow<List<Server>> = serverDao.getAllServers()

    override fun getFavoriteServers(): Flow<List<Server>> = serverDao.getFavoriteServers()

    override fun getServersByFolder(folder: ServerFolder): Flow<List<Server>> = serverDao.getServersByFolder(folder)

    override suspend fun getServerById(id: Int): Server? = serverDao.getServerById(id)

    override suspend fun insertServer(server: Server): Int {
        return serverDao.insertServer(server).toInt()
    }

    override suspend fun updateServer(server: Server) {
        serverDao.updateServer(server)
    }

    override suspend fun deleteServer(server: Server) {
        serverDao.deleteServer(server)
    }
}
