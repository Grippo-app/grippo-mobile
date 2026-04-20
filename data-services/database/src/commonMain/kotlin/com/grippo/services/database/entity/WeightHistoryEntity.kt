package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_history",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId")
    ]
)
public data class WeightHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val weight: Float,
    val createdAt: String,
    val updatedAt: String,
)