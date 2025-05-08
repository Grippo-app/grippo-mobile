package com.grippo.authorization.registration.excluded.muscles

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.muscles.models.MuscleGroupState
import com.grippo.presentation.api.muscles.models.MuscleRepresentation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ExcludedMusclesState(
    val suggestions: ImmutableList<MuscleGroupState<MuscleRepresentation.Plain>> = persistentListOf(),
    val selectedMuscleIds: PersistentList<String> = persistentListOf()
)