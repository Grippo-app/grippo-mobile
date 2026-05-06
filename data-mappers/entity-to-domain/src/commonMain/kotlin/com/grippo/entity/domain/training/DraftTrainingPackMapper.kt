package com.grippo.entity.domain.training

import com.grippo.data.features.api.training.models.DraftTraining
import com.grippo.services.database.models.DraftTrainingPack
import kotlin.time.Duration.Companion.minutes

public fun DraftTrainingPack.toDraftDomain(): DraftTraining {
    return DraftTraining(
        trainingId = training.trainingId,
        duration = training.duration.minutes,
        exercises = exercises.toDraftDomain(),
    )
}