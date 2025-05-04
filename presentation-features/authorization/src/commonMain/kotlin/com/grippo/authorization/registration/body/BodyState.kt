package com.grippo.authorization.registration.body

import androidx.compose.runtime.Immutable

@Immutable
internal data class BodyState(
    val weight: Float = 70.0F,
    val height: Int = 175,
)