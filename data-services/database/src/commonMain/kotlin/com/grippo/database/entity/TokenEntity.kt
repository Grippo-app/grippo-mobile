package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "token")
public data class TokenEntity(
    @PrimaryKey val id: String,
    val access: String? = null,
    val refresh: String? = null,
)