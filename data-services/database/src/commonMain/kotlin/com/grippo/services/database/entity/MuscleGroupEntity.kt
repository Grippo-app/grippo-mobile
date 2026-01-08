package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "muscle_group")
public data class MuscleGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val createdAt: String,
    val updatedAt: String,
)