package com.grippo.authorization.registration

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.auth.models.Experience

@Immutable
internal data class RegistrationState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val weight: Float = 0F,
    val height: Int = 0,
    val experience: Experience? = null
)