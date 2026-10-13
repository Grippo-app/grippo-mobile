package com.grippo.authorization.profile.creation

import androidx.compose.runtime.Immutable
import com.grippo.core.state.ImmutableListSerializer
import com.grippo.core.state.profile.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Immutable
@Serializable
internal data class ProfileCreationState(
    val name: String = "",
    val weight: Float = 0F,
    val height: Int = 0,
    val experience: ExperienceEnumState? = null,
    @Serializable(with = ImmutableListSerializer::class)
    val excludedMuscleIds: ImmutableList<String> = persistentListOf(),
    @Serializable(with = ImmutableListSerializer::class)
    val missingEquipmentIds: ImmutableList<String> = persistentListOf()
)
