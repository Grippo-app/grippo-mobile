package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_equipment",
    foreignKeys = [
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseExampleEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseExampleId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
public data class ExerciseEquipmentEntity(
    @PrimaryKey val id: String,
    val equipmentId: String,
    val exerciseExampleId: String,
    val createdAt: String,
    val updatedAt: String,
)