package com.grippo.authorization.registration.experience

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.user.models.Experience
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
internal data class ExperienceState(
    val suggestions: ImmutableList<Experience> = Experience.entries.toPersistentList(),
    val selected: Experience? = null
)