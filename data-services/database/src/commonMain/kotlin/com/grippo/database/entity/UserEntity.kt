package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user",
    foreignKeys = [
        ForeignKey(
            entity = TokenEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["id"])]
)
public data class UserEntity(
    @PrimaryKey val id: String,
    val weight: Float,
    val height: Int,
    val email: String,
    val experience: String,
    val name: String,
    val createdAt: String,
    val updatedAt: String,
)