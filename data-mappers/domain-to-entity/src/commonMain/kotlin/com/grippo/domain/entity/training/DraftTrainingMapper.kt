package com.grippo.domain.entity.training

import com.grippo.data.features.api.training.models.SetDraftTraining
import com.grippo.database.entity.DraftTrainingEntity
import com.grippo.database.models.DraftTrainingPack
import kotlin.uuid.Uuid

public fun SetDraftTraining.toEntity(
    userId: String
): DraftTrainingPack {

    val id = Uuid.random().toString()

    val training = DraftTrainingEntity(
        id = id,
        userId = userId,
        trainingId = trainingId,
        duration = training.duration.inWholeMinutes,
        volume = training.volume,
        repetitions = training.repetitions,
        intensity = training.intensity,
    )

    return DraftTrainingPack(
        training = training,
        exercises = this.training.exercises.map { it.toEntity(id) }
    )
}