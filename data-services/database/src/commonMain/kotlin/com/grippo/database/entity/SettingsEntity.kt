package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
public data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val theme: Theme = Theme.LIGHT,
    val locale: Locale = Locale.EN,
)

public enum class Theme {
    LIGHT,
    DARK
}

public enum class Locale {
    EN,
    UA
}