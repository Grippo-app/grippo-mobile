package com.grippo.authorization.registration.name

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.auth.models.NameFormatState

@Immutable
internal data class NameState(
    val name: NameFormatState = NameFormatState.of(""),
)