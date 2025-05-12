package com.grippo.authorization.registration

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.user.models.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class RegistrationState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val weight: Float = 0F,
    val height: Int = 0,
    val experience: ExperienceEnumState? = null,
    val excludedMuscleIds: ImmutableList<String> = persistentListOf(),
    val mussingEquipmentIds: ImmutableList<String> = persistentListOf()
)