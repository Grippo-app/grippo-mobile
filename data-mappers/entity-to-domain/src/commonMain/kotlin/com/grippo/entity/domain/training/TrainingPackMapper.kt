package com.grippo.entity.domain.training

import com.grippo.data.features.api.training.models.SetDraftTraining
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlin.time.Duration.Companion.minutes

public fun List<com.grippo.services.database.models.TrainingPack>.toDomain(): List<Training> {
    return map { it.toDomain() }
}

public fun com.grippo.services.database.models.TrainingPack.toDomain(): Training {
    return Training(
        id = training.id,
        exercises = exercises.toDomain(),
        volume = training.volume,
        repetitions = training.repetitions,
        intensity = training.intensity,
        duration = training.duration.minutes,
        createdAt = DateTimeUtils.toLocalDateTime(training.createdAt)
    )
}

public fun com.grippo.services.database.models.DraftTrainingPack.toSetDomain(): SetDraftTraining {
    val training = SetTraining(
        exercises = exercises.toSetDomain(),
        volume = training.volume,
        repetitions = training.repetitions,
        intensity = training.intensity,
        duration = training.duration.minutes,
    )

    return SetDraftTraining(
        trainingId = this.training.trainingId,
        training = training
    )
}