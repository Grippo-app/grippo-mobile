package com.grippo.domain.entity.training

import com.grippo.data.features.api.training.models.DraftTraining
import com.grippo.services.database.entity.DraftTrainingEntity
import com.grippo.services.database.models.DraftExercisePack
import com.grippo.services.database.models.DraftTrainingPack

public fun DraftTraining.toEntity(
    id: String,
    profileId: String,
    exercises: List<DraftExercisePack>,
): DraftTrainingPack {

    val training = DraftTrainingEntity(
        id = id,
        profileId = profileId,
        trainingId = trainingId,
        duration = duration.inWholeMinutes,
    )

    return DraftTrainingPack(
        training = training,
        exercises = exercises
    )
}
