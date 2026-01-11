package com.grippo.domain.entity.training

import com.grippo.data.features.api.training.models.SetDraftTraining
import com.grippo.services.database.entity.DraftTrainingEntity
import com.grippo.services.database.models.DraftTrainingPack
import kotlin.uuid.Uuid

public fun SetDraftTraining.toEntity(profileId: String): DraftTrainingPack {

    val id = Uuid.random().toString()

    val training = DraftTrainingEntity(
        id = id,
        profileId = profileId,
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
