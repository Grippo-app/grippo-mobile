package com.grippo.authorization.registration.name

import androidx.compose.runtime.Immutable
import com.grippo.state.formatters.NameFormatState

@Immutable
internal data class NameState(
    val name: NameFormatState = NameFormatState.of(""),
)