package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
public data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val theme: Theme = Theme.LIGHT
)

public enum class Theme {
    LIGHT,
    DARK
}