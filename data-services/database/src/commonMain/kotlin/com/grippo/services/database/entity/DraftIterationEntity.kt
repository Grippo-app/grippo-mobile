package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "draft_iteration",
    indices = [
        Index(value = ["exerciseId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DraftExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
public data class DraftIterationEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val externalWeight: Float?,
    val assistWeight: Float?,
    val extraWeight: Float?,
    val bodyWeight: Float?,
    val repetitions: Int,
)