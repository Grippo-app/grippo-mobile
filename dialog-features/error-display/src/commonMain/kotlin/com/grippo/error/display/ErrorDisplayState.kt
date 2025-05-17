package com.grippo.error.display

import androidx.compose.runtime.Immutable

@Immutable
public data class ErrorDisplayState(
    val title: String,
    val description: String,
)