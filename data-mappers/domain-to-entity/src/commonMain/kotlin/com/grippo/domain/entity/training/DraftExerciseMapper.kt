package com.grippo.domain.entity.training

import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlin.uuid.Uuid

public fun SetExercise.toEntity(trainingId: String): com.grippo.services.database.models.DraftExercisePack {

    val id = Uuid.random().toString()

    val exercise = _root_ide_package_.com.grippo.services.database.entity.DraftExerciseEntity(
        id = id,
        name = name,
        trainingId = trainingId,
        volume = volume,
        repetitions = repetitions,
        intensity = intensity,
        exerciseExampleId = exerciseExample.id,
        createdAt = DateTimeUtils.toUtcIso(createdAt)
    )

    return _root_ide_package_.com.grippo.services.database.models.DraftExercisePack(
        exercise = exercise,
        iterations = iterations.map { it.toEntity(id) },
        example = null
    )
}