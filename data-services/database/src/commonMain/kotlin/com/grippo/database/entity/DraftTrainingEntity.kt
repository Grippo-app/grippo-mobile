package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "draft_training",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
)
public data class DraftTrainingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val duration: Long,
    val volume: Float,
    val repetitions: Int,
    val intensity: Float,
)