package com.grippo.database.domain.training

import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.database.models.DraftTrainingPack
import com.grippo.database.models.TrainingPack
import com.grippo.date.utils.DateTimeUtils

public fun List<TrainingPack>.toDomain(): List<Training> {
    return map { it.toDomain() }
}

public fun TrainingPack.toDomain(): Training {
    return Training(
        id = training.id,
        exercises = exercises.toDomain(),
        volume = training.volume,
        repetitions = training.repetitions,
        intensity = training.intensity,
        duration = training.duration,
        createdAt = DateTimeUtils.toLocalDateTime(training.createdAt)
    )
}

public fun DraftTrainingPack.toSetDomain(): SetTraining {
    return SetTraining(
        exercises = exercises.toSetDomain(),
        volume = training.volume,
        repetitions = training.repetitions,
        intensity = training.intensity,
        duration = training.duration,
    )
}