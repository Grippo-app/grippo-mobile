package com.grippo.services.database.models

import androidx.room.Embedded
import androidx.room.Relation
import com.grippo.services.database.entity.ExerciseEntity
import com.grippo.services.database.entity.ExerciseExampleEntity
import com.grippo.services.database.entity.IterationEntity
import com.grippo.services.database.entity.TrainingEntity

public data class TrainingPack(
    @Embedded val training: TrainingEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "trainingId",
        entity = ExerciseEntity::class
    )
    val exercises: List<ExercisePack> = emptyList()
)

public data class ExercisePack(
    @Embedded val exercise: ExerciseEntity,

    @Relation(
        parentColumn = "exerciseExampleId",
        entityColumn = "id"
    )
    val example: ExerciseExampleEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId",
        entity = IterationEntity::class
    )
    val iterations: List<IterationEntity> = emptyList()
)
