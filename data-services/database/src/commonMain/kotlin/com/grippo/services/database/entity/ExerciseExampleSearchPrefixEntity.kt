package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "exercise_example_search_prefix",
    primaryKeys = ["exerciseExampleId", "token", "prefix"],
    indices = [
        Index(value = ["exerciseExampleId"]),
        Index(value = ["prefix"])
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
public data class ExerciseExampleSearchPrefixEntity(
    val exerciseExampleId: String,
    val token: String,
    val prefix: String,
)
