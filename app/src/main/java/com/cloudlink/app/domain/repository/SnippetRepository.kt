package com.cloudlink.app.domain.repository

import com.cloudlink.app.data.model.CommandSnippet
import kotlinx.coroutines.flow.Flow

interface SnippetRepository {
    fun getAllSnippets(): Flow<List<CommandSnippet>>
    suspend fun insertSnippet(snippet: CommandSnippet)
    suspend fun deleteSnippet(snippet: CommandSnippet)
}
