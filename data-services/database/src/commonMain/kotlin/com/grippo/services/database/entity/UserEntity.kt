package com.grippo.services.database.entity

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
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["profileId"], unique = true)
    ]
)
public data class UserEntity(
    // Account
    @PrimaryKey val id: String,
    val email: String,
    val createdAt: String,
    val updatedAt: String,

    // Profile
    val profileId: String,
    val weight: Float,
    val height: Int,
    val experience: String,
    val name: String,
)
