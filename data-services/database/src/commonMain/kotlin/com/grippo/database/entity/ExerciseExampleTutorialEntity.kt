package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_example_tutorial",
    indices = [
        Index(value = ["exerciseExampleId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseExampleEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseExampleId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
public data class ExerciseExampleTutorialEntity(
    @PrimaryKey val id: String,
    val exerciseExampleId: String,
    val title: String,
    val language: String,
    val author: String?,
    val value: String,
    val resourceType: String,
    val createdAt: String,
    val updatedAt: String,
)