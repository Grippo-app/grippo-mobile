package com.grippo.authorization.registration.experience

import androidx.compose.runtime.Immutable
import com.grippo.state.profile.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
internal data class ExperienceState(
    val suggestions: ImmutableList<ExperienceEnumState> = ExperienceEnumState.entries.toPersistentList(),
    val selected: ExperienceEnumState? = null
)