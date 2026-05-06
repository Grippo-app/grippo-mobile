package com.grippo.domain.entity.training

import com.grippo.data.features.api.training.models.DraftExercise
import com.grippo.services.database.entity.DraftExerciseEntity
import com.grippo.services.database.models.DraftExercisePack
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlin.uuid.Uuid

public fun DraftExercise.toEntity(trainingId: String): DraftExercisePack {
    val id = Uuid.random().toString()

    // Aggregate columns (volume / repetitions / intensity) are vestigial — see
    // DraftTrainingMapper.toEntity for the rationale.
    val exercise = DraftExerciseEntity(
        id = id,
        name = name,
        trainingId = trainingId,
        volume = 0f,
        repetitions = 0,
        intensity = 0f,
        exerciseExampleId = exerciseExample.id,
        createdAt = DateTimeUtils.toUtcIso(createdAt)
    )

    return DraftExercisePack(
        exercise = exercise,
        iterations = iterations.map { it.toEntity(id) },
        example = null
    )
}
