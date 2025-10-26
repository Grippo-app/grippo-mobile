package com.grippo.domain.entity.training

import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.database.entity.DraftExerciseEntity
import com.grippo.database.models.DraftExercisePack
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlin.uuid.Uuid

public fun SetExercise.toEntity(trainingId: String): DraftExercisePack {

    val id = Uuid.random().toString()

    val exercise = DraftExerciseEntity(
        id = id,
        name = name,
        trainingId = trainingId,
        volume = volume,
        repetitions = repetitions,
        intensity = intensity,
        exerciseExampleId = exerciseExample.id,
        createdAt = DateTimeUtils.toUtcIso(createdAt)
    )

    return DraftExercisePack(
        exercise = exercise,
        iterations = iterations.map { it.toEntity(id) },
        example = null
    )
}