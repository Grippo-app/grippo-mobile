package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "goal",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
public data class GoalEntity(
    @PrimaryKey val userId: String,
    val primaryGoal: String,
    val secondaryGoal: String?,
    val target: String,
    val personalizations: List<String>,
    val confidence: Double,
    val createdAt: String,
    val updatedAt: String,
    val lastConfirmedAt: String?,
)
