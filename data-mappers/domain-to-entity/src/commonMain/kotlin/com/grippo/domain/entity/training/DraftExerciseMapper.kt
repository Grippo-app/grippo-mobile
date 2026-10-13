package com.grippo.domain.entity.training

import com.grippo.data.features.api.training.models.DraftExercise
import com.grippo.services.database.entity.DraftExerciseEntity
import com.grippo.services.database.entity.DraftIterationEntity
import com.grippo.services.database.models.DraftExercisePack
import com.grippo.toolkit.date.utils.DateTimeUtils

public fun DraftExercise.toEntity(
    id: String,
    trainingId: String,
    iterations: List<DraftIterationEntity>,
): DraftExercisePack {

    val exercise = DraftExerciseEntity(
        id = id,
        trainingId = trainingId,
        exerciseExampleId = exerciseExample.id,
        createdAt = DateTimeUtils.toUtcIso(createdAt),
    )

    return DraftExercisePack(
        exercise = exercise,
        iterations = iterations,
        example = null,
    )
}
