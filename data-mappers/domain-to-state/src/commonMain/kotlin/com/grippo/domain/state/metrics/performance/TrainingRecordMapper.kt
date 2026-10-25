package com.grippo.domain.state.metrics.performance

import com.grippo.core.state.metrics.performance.TrainingRecordState
import com.grippo.data.features.api.metrics.performance.models.TrainingRecord
import com.grippo.domain.state.achievements.toState
import com.grippo.domain.state.exercise.example.toState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<TrainingRecord>.toState(): ImmutableList<TrainingRecordState> {
    return map { it.toState() }.toPersistentList()
}

public fun TrainingRecord.toState(): TrainingRecordState {
    return TrainingRecordState(
        exerciseExample = exerciseExample.toState(),
        achievements = achievements.toState().toPersistentList(),
    )
}
