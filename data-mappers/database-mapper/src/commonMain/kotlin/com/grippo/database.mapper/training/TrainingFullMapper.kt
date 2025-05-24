package com.grippo.database.mapper.training

import com.grippo.data.features.api.training.models.Training
import com.grippo.database.models.TrainingFull
import com.grippo.date.utils.DateTimeUtils

public fun List<TrainingFull>.toDomain(): List<Training> {
    return mapNotNull { it.toDomain() }
}

public fun TrainingFull.toDomain(): Training {
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