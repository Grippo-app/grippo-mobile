package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_history")
public data class WeightHistoryEntity(
    @PrimaryKey val id: String,
    val weight: Float,
    val createdAt: String,
    val updatedAt: String,
)