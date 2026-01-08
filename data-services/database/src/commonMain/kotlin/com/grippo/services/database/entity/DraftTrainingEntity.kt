package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "draft_training",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
)
public data class DraftTrainingEntity(
    @PrimaryKey val id: String,
    val trainingId: String?, // [null for "Add"] [nonNull for "Edit"]
    val profileId: String,
    val duration: Long,
    val volume: Float,
    val repetitions: Int,
    val intensity: Float,
)
