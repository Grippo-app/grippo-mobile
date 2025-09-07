package com.grippo.database.models

import androidx.room.Embedded
import androidx.room.Relation
import com.grippo.database.entity.DraftExerciseEntity
import com.grippo.database.entity.DraftIterationEntity
import com.grippo.database.entity.DraftTrainingEntity
import com.grippo.database.entity.ExerciseExampleEntity

public data class DraftTrainingPack(
    @Embedded val training: DraftTrainingEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "trainingId",
        entity = DraftExerciseEntity::class
    )
    val exercises: List<DraftExercisePack> = emptyList()
)

public data class DraftExercisePack(
    @Embedded val exercise: DraftExerciseEntity,

    @Relation(
        parentColumn = "exerciseExampleId",
        entityColumn = "id"
    )
    val example: ExerciseExampleEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId",
        entity = DraftIterationEntity::class
    )
    val iterations: List<DraftIterationEntity> = emptyList()
)