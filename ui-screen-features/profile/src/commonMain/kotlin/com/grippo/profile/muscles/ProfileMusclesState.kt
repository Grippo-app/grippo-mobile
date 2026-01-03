package com.grippo.profile.muscles

import androidx.compose.runtime.Immutable
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ProfileMusclesState(
    val suggestions: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>> = persistentListOf(),
    val selectedMuscleIds: PersistentList<String> = persistentListOf(),
)
