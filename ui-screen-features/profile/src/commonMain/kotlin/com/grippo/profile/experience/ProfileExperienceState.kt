package com.grippo.profile.experience

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
internal data class ProfileExperienceState(
    val suggestions: ImmutableList<ExperienceEnumState> = ExperienceEnumState.entries.toPersistentList(),
    val selected: ExperienceEnumState? = null
)
