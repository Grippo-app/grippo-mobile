package com.grippo.authorization.profile.creation.name

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.NameFormatState

@Immutable
internal data class NameState(
    val name: NameFormatState = NameFormatState.of(""),
)