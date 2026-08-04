package com.cloudlink.app.data.database

import androidx.room.TypeConverter
import com.cloudlink.app.data.model.AuthType
import com.cloudlink.app.data.model.LogType
import com.cloudlink.app.data.model.ServerFolder

class Converters {
    @TypeConverter
    fun fromAuthType(value: AuthType) = value.name

    @TypeConverter
    fun toAuthType(value: String) = enumValueOf<AuthType>(value)

    @TypeConverter
    fun fromServerFolder(value: ServerFolder) = value.name

    @TypeConverter
    fun toServerFolder(value: String) = enumValueOf<ServerFolder>(value)

    @TypeConverter
    fun fromLogType(value: LogType) = value.name

    @TypeConverter
    fun toLogType(value: String) = enumValueOf<LogType>(value)
}
