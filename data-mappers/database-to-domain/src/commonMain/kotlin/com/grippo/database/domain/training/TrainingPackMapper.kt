package com.grippo.database.domain.training

import com.grippo.data.features.api.training.models.SetDraftTraining
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.database.models.DraftTrainingPack
import com.grippo.database.models.TrainingPack
import com.grippo.date.utils.DateTimeUtils
import kotlin.time.Duration.Companion.minutes

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
        duration = training.duration.minutes,
        createdAt = DateTimeUtils.toLocalDateTime(training.createdAt)
    )
}

public fun DraftTrainingPack.toSetDomain(): SetDraftTraining {
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