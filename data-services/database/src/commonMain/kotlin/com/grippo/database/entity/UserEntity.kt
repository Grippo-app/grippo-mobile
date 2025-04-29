package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
public data class UserEntity(
    @PrimaryKey val id: String,
    val weight: Float,
    val height: Float,
    val email: String,
    val experience: String,
    val name: String,
    val createdAt: String,
    val updatedAt: String,
)