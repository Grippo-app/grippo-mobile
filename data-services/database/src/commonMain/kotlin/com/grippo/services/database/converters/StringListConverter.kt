package com.grippo.services.database.converters

import androidx.room.TypeConverter

public class StringListConverter {

    @TypeConverter
    public fun fromList(list: List<String>): String {
        return list.joinToString(separator = "|")
    }

    @TypeConverter
    public fun toList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value.split("|")
    }
}
