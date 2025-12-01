package com.grippo.authorization.profile.creation

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class RegistrationState(
    val name: String = "",
    val weight: Float = 0F,
    val height: Int = 0,
    val experience: ExperienceEnumState? = null,
    val excludedMuscleIds: ImmutableList<String> = persistentListOf(),
    val missingEquipmentIds: ImmutableList<String> = persistentListOf()
)
