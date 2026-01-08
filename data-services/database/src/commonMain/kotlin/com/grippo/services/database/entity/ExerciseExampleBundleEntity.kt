package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_example_bundle",
    indices = [
        Index(value = ["exerciseExampleId"]),
        Index(value = ["muscleId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseExampleEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseExampleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MuscleEntity::class,
            parentColumns = ["id"],
            childColumns = ["muscleId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
public data class ExerciseExampleBundleEntity(
    @PrimaryKey val id: String,
    val exerciseExampleId: String,
    val muscleId: String,
    val percentage: Int,
    val createdAt: String,
    val updatedAt: String,
)