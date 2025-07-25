package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
public data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val colorMode: ColorMode = ColorMode.LIGHT
)

public enum class ColorMode {
    LIGHT,
    DARK
}