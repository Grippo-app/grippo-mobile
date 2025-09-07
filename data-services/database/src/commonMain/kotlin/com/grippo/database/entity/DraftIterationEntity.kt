package com.grippo.database.entity

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
    val volume: Float,
    val repetitions: Int,
)