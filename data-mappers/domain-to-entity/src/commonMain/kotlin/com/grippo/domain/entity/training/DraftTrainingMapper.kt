package com.grippo.domain.entity.training

import com.grippo.data.features.api.training.models.DraftTraining
import com.grippo.services.database.entity.DraftTrainingEntity
import com.grippo.services.database.models.DraftTrainingPack
import kotlin.uuid.Uuid

public fun DraftTraining.toEntity(profileId: String): DraftTrainingPack {
    val id = Uuid.random().toString()

    val training = DraftTrainingEntity(
        id = id,
        profileId = profileId,
        trainingId = trainingId,
        duration = duration.inWholeMinutes,
    )

    return DraftTrainingPack(
        training = training,
        exercises = exercises.map { it.toEntity(id) }
    )
}
