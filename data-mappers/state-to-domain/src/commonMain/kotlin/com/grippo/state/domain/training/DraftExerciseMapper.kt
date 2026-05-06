package com.grippo.state.domain.training

import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.training.models.DraftExercise
import com.grippo.state.domain.example.toDomain
import com.grippo.toolkit.logger.AppLogger
import kotlinx.collections.immutable.toPersistentList

public fun List<ExerciseState>.toDraftDomain(): List<DraftExercise> {
    return mapNotNull { it.toDraftDomain() }.toPersistentList()
}

public fun ExerciseState.toDraftDomain(): DraftExercise? {
    val mappedCreatedAt = AppLogger.Mapping.log(createdAt.value) {
        "ExerciseState createdAt value is null (id=$id, draft path)"
    } ?: return null

    return DraftExercise(
        iterations = iterations.toDomain(),
        exerciseExample = exerciseExample.toDomain(),
        createdAt = mappedCreatedAt,
    )
}
