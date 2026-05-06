package com.grippo.domain.entity.training

import com.grippo.data.features.api.training.models.DraftTraining
import com.grippo.services.database.entity.DraftTrainingEntity
import com.grippo.services.database.models.DraftTrainingPack
import kotlin.uuid.Uuid

public fun DraftTraining.toEntity(profileId: String): DraftTrainingPack {
    val id = Uuid.random().toString()

    // Drafts are aggregate-free in the domain. The entity columns below are
    // legacy artifacts of the previous SetTraining-shaped draft and are kept
    // at zero — they are not read back into the domain model.
    val training = DraftTrainingEntity(
        id = id,
        profileId = profileId,
        trainingId = trainingId,
        duration = duration.inWholeMinutes,
        volume = 0f,
        repetitions = 0,
        intensity = 0f,
    )

    return DraftTrainingPack(
        training = training,
        exercises = exercises.map { it.toEntity(id) }
    )
}
