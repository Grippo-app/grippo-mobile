package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "muscle",
    indices = [
        Index(value = ["muscleGroupId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = MuscleGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["muscleGroupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
public data class MuscleEntity(
    @PrimaryKey val id: String,
    val muscleGroupId: String,
    val name: String,
    val type: String,
    val recovery: Int,
    val size: Float,
    val sensitivity: Float,
    val createdAt: String,
    val updatedAt: String,
)