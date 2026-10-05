package com.cloudlink.app.data.database

import androidx.room.*
import com.cloudlink.app.data.model.CommandSnippet
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandSnippetDao {
    @Query("SELECT * FROM snippets ORDER BY name ASC")
    fun getAllSnippets(): Flow<List<CommandSnippet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: CommandSnippet)

    @Update
    suspend fun updateSnippet(snippet: CommandSnippet)

    @Delete
    suspend fun deleteSnippet(snippet: CommandSnippet)
}
