package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "muscle",
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
    val createdAt: String,
    val updatedAt: String,
)