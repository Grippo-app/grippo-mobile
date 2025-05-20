package com.grippo.network.mapper

import com.grippo.database.models.ExerciseFull
import com.grippo.database.models.TrainingFull
import com.grippo.logger.AppLogger
import com.grippo.network.dto.TrainingResponse

public fun List<TrainingResponse>.toTrainingFullList(): List<TrainingFull> {
    return mapNotNull { it.toTrainingFullOrNull() }
}

public fun TrainingResponse.toTrainingFullOrNull(): TrainingFull? {
    val trainingEntity = AppLogger.checkOrLog(
        value = toEntityOrNull(),
        msg = { "TrainingDto.toEntityOrNull() returned null" }
    ) ?: return null

    val exerciseFullList = exercises.mapNotNull { exercise ->
        val exerciseEntity = AppLogger.checkOrLog(
            value = exercise.toEntityOrNull(),
            msg = { "ExerciseDto.toEntityOrNull() returned null" }
        ) ?: return@mapNotNull null

        val iterationEntities = exercise.iterations.mapNotNull { iteration ->
            AppLogger.checkOrLog(
                value = iteration.toEntityOrNull(),
                msg = { "IterationDto.toEntityOrNull() returned null" }
            )
        }

        ExerciseFull(
            exercise = exerciseEntity,
            example = null,
            iterations = iterationEntities
        )
    }

    return TrainingFull(
        training = trainingEntity,
        exercises = exerciseFullList
    )
}