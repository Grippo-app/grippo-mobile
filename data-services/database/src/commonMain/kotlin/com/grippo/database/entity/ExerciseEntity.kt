package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise",
    indices = [
        Index(value = ["trainingId"]),
        Index(value = ["exerciseExampleId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = TrainingEntity::class,
            parentColumns = ["id"],
            childColumns = ["trainingId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
public data class ExerciseEntity(
    @PrimaryKey val id: String,
    val trainingId: String,
    val exerciseExampleId: String,
    val name: String,
    val volume: Float,
    val repetitions: Int,
    val intensity: Float,
    val createdAt: String,
    val updatedAt: String,
)