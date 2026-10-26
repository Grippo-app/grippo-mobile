package com.grippo.profile.experience

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Immutable
@Serializable
internal data class ProfileExperienceState(
    @Transient
    val suggestions: ImmutableList<ExperienceEnumState> = ExperienceEnumState.entries.toPersistentList(),
    val selected: ExperienceEnumState? = null,
)
