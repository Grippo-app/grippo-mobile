package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "training",
    indices = [Index(value = ["profileId"])],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
)
public data class TrainingEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val duration: Long,
    val createdAt: String,
    val volume: Float,
    val repetitions: Int,
    val intensity: Float,
    val updatedAt: String,
)
