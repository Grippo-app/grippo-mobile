package com.grippo.profile.muscles

import androidx.compose.runtime.Immutable
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.design.resources.provider.muscles.MuscleColorPreset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

@Immutable
internal data class ProfileMusclesState(
    val suggestions: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>> = persistentListOf(),
    val selectedMuscleIds: PersistentList<String> = persistentListOf(),
    val musclePresets: PersistentMap<String, MuscleColorPreset> = persistentMapOf(),
)
