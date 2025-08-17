package com.grippo.training.preferences

import com.grippo.core.models.BaseLoader
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class TrainingPreferencesState(
    val selectedMuscleIds: Set<String> = emptySet(),
    val muscles: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>> = persistentListOf(),
)





