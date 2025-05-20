package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training")
public data class TrainingEntity(
    @PrimaryKey val id: String,
    val duration: Long,
    val createdAt: String,
    val volume: Float,
    val repetitions: Int,
    val intensity: Float,
    val updatedAt: String,
)