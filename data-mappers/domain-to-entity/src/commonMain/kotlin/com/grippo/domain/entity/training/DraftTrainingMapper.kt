package com.grippo.domain.entity.training

import com.grippo.data.features.api.training.models.SetDraftTraining
import kotlin.uuid.Uuid

public fun SetDraftTraining.toEntity(
    profileId: String
): com.grippo.services.database.models.DraftTrainingPack {

    val id = Uuid.random().toString()

    val training = _root_ide_package_.com.grippo.services.database.entity.DraftTrainingEntity(
        id = id,
        profileId = profileId,
        trainingId = trainingId,
        duration = training.duration.inWholeMinutes,
        volume = training.volume,
        repetitions = training.repetitions,
        intensity = training.intensity,
    )

    return _root_ide_package_.com.grippo.services.database.models.DraftTrainingPack(
        training = training,
        exercises = this.training.exercises.map { it.toEntity(id) }
    )
}
