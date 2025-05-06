package com.grippo.authorization.registration

import androidx.compose.runtime.Immutable

@Immutable
internal data class RegistrationState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val weight: Float = 0F,
    val height: Int = 0,
)