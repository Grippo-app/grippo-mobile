package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "draft_exercise",
    indices = [
        Index(value = ["trainingId"]),
        Index(value = ["exerciseExampleId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DraftTrainingEntity::class,
            parentColumns = ["id"],
            childColumns = ["trainingId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
public data class DraftExerciseEntity(
    @PrimaryKey val id: String,
    val trainingId: String,
    val exerciseExampleId: String,
    val name: String,
    val volume: Float,
    val repetitions: Int,
    val createdAt: String,
    val intensity: Float,
)