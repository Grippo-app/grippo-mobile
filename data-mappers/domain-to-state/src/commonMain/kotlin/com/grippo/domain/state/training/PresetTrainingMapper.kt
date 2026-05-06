package com.grippo.domain.state.training

import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.training.models.PresetTraining
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

public fun PresetTraining.toState(): PersistentList<ExerciseState> {
    return exercises.map { it.toState() }.toPersistentList()
}
