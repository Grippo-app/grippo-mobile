package com.grippo.error.display

import androidx.compose.runtime.Immutable
import com.grippo.core.state.error.AppErrorState

@Immutable
public data class ErrorDisplayState(
    val error: AppErrorState
)