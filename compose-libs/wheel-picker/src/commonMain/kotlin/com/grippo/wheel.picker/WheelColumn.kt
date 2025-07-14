package com.grippo.wheel.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

@Immutable
public data class WheelColumn<T>(
    val id: String,
    val items: List<T>,
    val initialIndex: Int,
    val onValueChange: (T) -> Unit,
    val itemContent: @Composable (T) -> Unit,
    val descriptionContent: (@Composable () -> Unit)? = null,
)