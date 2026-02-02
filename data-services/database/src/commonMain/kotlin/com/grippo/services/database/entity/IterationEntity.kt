package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "iteration",
    indices = [
        Index(value = ["exerciseId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
public data class IterationEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val externalWeight: Float?,
    val assistWeight: Float?,
    val extraWeight: Float?,
    val bodyWeight: Float?,
    val repetitions: Int,
    val createdAt: String,
    val updatedAt: String,
)