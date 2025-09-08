package com.grippo.domain.database.settings.training

import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.database.entity.DraftTrainingEntity
import com.grippo.database.models.DraftTrainingPack
import kotlin.uuid.Uuid

public fun SetTraining.toEntity(userId: String): DraftTrainingPack {

    val id = Uuid.random().toString()

    val training = DraftTrainingEntity(
        id = id,
        userId = userId,
        duration = duration.inWholeMinutes,
        volume = volume,
        repetitions = repetitions,
        intensity = intensity,
    )

    return DraftTrainingPack(
        training = training,
        exercises = exercises.map { it.toEntity(id) }
    )
}