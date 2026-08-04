package com.cloudlink.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "servers",
    indices = [
        Index("folder"),
        Index("favorite")
    ]
)
data class Server(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: AuthType = AuthType.PASSWORD,
    val folder: ServerFolder = ServerFolder.HOME_LAB,
    val favorite: Boolean = false,
    val notes: String = "",
    val tags: String = "" // Comma separated tags
)

enum class AuthType {
    PASSWORD, KEY
}

enum class ServerFolder {
    ALL, HOME_LAB, RASPBERRY_PIS, VPS, SCHOOL, DEVELOPMENT, PRODUCTION;

    val displayName: String
        get() = name.lowercase().split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

@Entity(tableName = "snippets")
data class CommandSnippet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val command: String,
    val category: String = "General"
)

@Entity(
    tableName = "logs",
    foreignKeys = [
        ForeignKey(
            entity = Server::class,
            parentColumns = ["id"],
            childColumns = ["serverId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("serverId"),
        Index("timestamp")
    ]
)
data class ConnectionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serverId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val type: LogType = LogType.SYSTEM
)

enum class LogType {
    TERMINAL, SYSTEM, ERROR
}
