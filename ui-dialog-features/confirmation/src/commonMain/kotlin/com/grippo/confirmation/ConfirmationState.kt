package com.grippo.confirmation

import androidx.compose.runtime.Immutable

@Immutable
public data class ConfirmationState(
    val title: String,
    val description: String?,
)
