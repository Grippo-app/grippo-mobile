package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_example_components",
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
public data class ExerciseExampleComponentsEntity(
    @PrimaryKey val exerciseExampleId: String,
    val assistWeightRequired: Boolean?,
    val bodyWeightRequired: Boolean?,
    val bodyWeightMultiplier: Double?,
    val externalWeightRequired: Boolean?,
    val extraWeightRequired: Boolean?,
)
