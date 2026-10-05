package com.cloudlink.app.di

import android.content.Context
import androidx.room.Room
import com.cloudlink.app.data.database.AppDatabase
import com.cloudlink.app.data.database.CommandSnippetDao
import com.cloudlink.app.data.database.ConnectionLogDao
import com.cloudlink.app.data.database.ServerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "cloudlink_database"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()
    }

    @Provides
    fun provideServerDao(appDatabase: AppDatabase): ServerDao {
        return appDatabase.serverDao()
    }

    @Provides
    fun provideCommandSnippetDao(appDatabase: AppDatabase): CommandSnippetDao {
        return appDatabase.snippetDao()
    }

    @Provides
    fun provideConnectionLogDao(appDatabase: AppDatabase): ConnectionLogDao {
        return appDatabase.logDao()
    }
}
