package com.grippo.authorization.registration.name

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.auth.models.Name

@Immutable
internal data class NameState(
    val name: Name = Name.of(""),
)