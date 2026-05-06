package com.grippo.entity.domain.training

import com.grippo.data.features.api.training.models.DraftExercise
import com.grippo.entity.domain.equipment.toDomain
import com.grippo.services.database.models.DraftExercisePack
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.logger.AppLogger

public fun List<DraftExercisePack>.toDraftDomain(): List<DraftExercise> {
    return mapNotNull { it.toDraftDomain() }
}

public fun DraftExercisePack.toDraftDomain(): DraftExercise? {
    val mappedExerciseExample = AppLogger.Mapping.log(example?.toDomain()) {
        "DraftExercisePack exercise by ${exercise.exerciseExampleId} is null"
    } ?: return null

    return DraftExercise(
        iterations = iterations.toSetDomain(),
        createdAt = DateTimeUtils.toLocalDateTime(exercise.createdAt),
        exerciseExample = mappedExerciseExample,
    )
}
