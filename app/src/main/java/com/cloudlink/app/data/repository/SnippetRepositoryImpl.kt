package com.cloudlink.app.data.repository

import com.cloudlink.app.data.database.CommandSnippetDao
import com.cloudlink.app.data.model.CommandSnippet
import com.cloudlink.app.domain.repository.SnippetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SnippetRepositoryImpl @Inject constructor(
    private val snippetDao: CommandSnippetDao
) : SnippetRepository {
    override fun getAllSnippets(): Flow<List<CommandSnippet>> = snippetDao.getAllSnippets()

    override suspend fun insertSnippet(snippet: CommandSnippet) {
        snippetDao.insertSnippet(snippet)
    }

    override suspend fun deleteSnippet(snippet: CommandSnippet) {
        snippetDao.deleteSnippet(snippet)
    }
}
