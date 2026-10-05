package com.cloudlink.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cloudlink.app.data.model.CommandSnippet
import com.cloudlink.app.data.model.ConnectionLog
import com.cloudlink.app.data.model.Server

@Database(
    entities = [Server::class, CommandSnippet::class, ConnectionLog::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun snippetDao(): CommandSnippetDao
    abstract fun logDao(): ConnectionLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `servers_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `host` TEXT NOT NULL,
                        `port` INTEGER NOT NULL,
                        `username` TEXT NOT NULL,
                        `authType` TEXT NOT NULL,
                        `folder` TEXT NOT NULL,
                        `favorite` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `tags` TEXT NOT NULL
                    )
                """)

                val serverColumns = tableColumns(db, "servers")
                if (serverColumns.isNotEmpty()) {
                    db.execSQL("""
                        INSERT INTO `servers_new` (`id`, `name`, `host`, `port`, `username`, `authType`, `folder`, `favorite`, `notes`, `tags`)
                        SELECT
                            ${columnOrDefault(serverColumns, "id", "NULL")},
                            ${columnOrDefault(serverColumns, "name", "''")},
                            ${columnOrDefault(serverColumns, "host", "''")},
                            ${columnOrDefault(serverColumns, "port", "22")},
                            ${columnOrDefault(serverColumns, "username", "''")},
                            ${columnOrDefault(serverColumns, "authType", "'PASSWORD'")},
                            ${columnOrDefault(serverColumns, "folder", "'HOME_LAB'")},
                            ${columnOrDefault(serverColumns, "favorite", "0")},
                            ${columnOrDefault(serverColumns, "notes", "''")},
                            ${columnOrDefault(serverColumns, "tags", "''")}
                        FROM `servers`
                    """)
                }

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `snippets_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `command` TEXT NOT NULL,
                        `category` TEXT NOT NULL
                    )
                """)

                val snippetColumns = tableColumns(db, "snippets")
                if (snippetColumns.isNotEmpty()) {
                    db.execSQL("""
                        INSERT INTO `snippets_new` (`id`, `name`, `command`, `category`)
                        SELECT
                            ${columnOrDefault(snippetColumns, "id", "NULL")},
                            ${columnOrDefault(snippetColumns, "name", "''")},
                            ${columnOrDefault(snippetColumns, "command", "''")},
                            ${columnOrDefault(snippetColumns, "category", "'General'")}
                        FROM `snippets`
                    """)
                }

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `logs_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `serverId` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `message` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        FOREIGN KEY(`serverId`) REFERENCES `servers_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)

                val logColumns = tableColumns(db, "logs")
                if ("serverId" in logColumns) {
                    db.execSQL("""
                        INSERT INTO `logs_new` (`id`, `serverId`, `timestamp`, `message`, `type`)
                        SELECT
                            ${columnOrDefault(logColumns, "id", "NULL")},
                            `serverId`,
                            ${columnOrDefault(logColumns, "timestamp", "0")},
                            ${columnOrDefault(logColumns, "message", "''")},
                            ${columnOrDefault(logColumns, "type", "'SYSTEM'")}
                        FROM `logs`
                        WHERE `serverId` IN (SELECT `id` FROM `servers_new`)
                    """)
                }

                db.execSQL("DROP TABLE IF EXISTS `logs`")
                db.execSQL("DROP TABLE IF EXISTS `snippets`")
                db.execSQL("DROP TABLE IF EXISTS `servers`")
                db.execSQL("ALTER TABLE `servers_new` RENAME TO `servers`")
                db.execSQL("ALTER TABLE `snippets_new` RENAME TO `snippets`")
                db.execSQL("ALTER TABLE `logs_new` RENAME TO `logs`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_servers_folder` ON `servers` (`folder`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_servers_favorite` ON `servers` (`favorite`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_logs_serverId` ON `logs` (`serverId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_logs_timestamp` ON `logs` (`timestamp`)")
            }
        }

        private fun tableColumns(db: SupportSQLiteDatabase, tableName: String): Set<String> {
            return db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                buildSet {
                    while (cursor.moveToNext()) {
                        if (nameIndex >= 0) add(cursor.getString(nameIndex))
                    }
                }
            }
        }

        private fun columnOrDefault(
            columns: Set<String>,
            columnName: String,
            defaultExpression: String
        ): String = if (columnName in columns) "`$columnName`" else defaultExpression

    }
}
