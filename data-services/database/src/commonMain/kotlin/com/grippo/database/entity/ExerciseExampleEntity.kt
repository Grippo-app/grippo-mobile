package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_example")
public data class ExerciseExampleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val createdAt: String,
    val updatedAt: String,
    val forceType: String,
    val weightType: String,
    val category: String,
    val experience: String,
    val imageUrl: String?,
)