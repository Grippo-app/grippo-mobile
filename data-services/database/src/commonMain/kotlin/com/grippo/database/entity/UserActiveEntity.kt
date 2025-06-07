package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_active")
public data class UserActiveEntity(
    @PrimaryKey val id: Int = 0,
    val userId: String
)