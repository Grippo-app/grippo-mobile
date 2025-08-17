package com.grippo.training.setup

import androidx.compose.runtime.Immutable
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class TrainingSetupState(
    val selectedMuscleIds: Set<String> = emptySet(),
    val muscles: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>> = persistentListOf(),
)