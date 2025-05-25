package com.grippo.domain.mapper.exercise.example

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleBundle
import com.grippo.domain.mapper.muscles.toState
import com.grippo.logger.AppLogger
import com.grippo.presentation.api.exercise.example.models.ExerciseExampleBundleState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<ExerciseExampleBundle>.toState(): ImmutableList<ExerciseExampleBundleState> {
    return mapNotNull { it.toState() }.toPersistentList()
}

public fun ExerciseExampleBundle.toState(): ExerciseExampleBundleState? {
    val mappedMuscle = AppLogger.checkOrLog(muscle.toState()?.value) {
        "ExerciseExampleBundle $id has unrecognized muscle: $muscle"
    } ?: return null

    return ExerciseExampleBundleState(
        id = id,
        percentage = percentage,
        muscle = mappedMuscle
    )
}