package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_stats",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
public data class UserStatsEntity(
    @PrimaryKey val userId: String,
    val trainingsCount: Int,
    val totalDuration: Int,
    val totalVolume: Float,
    val totalRepetitions: Int,
)
