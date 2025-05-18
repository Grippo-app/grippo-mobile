package com.grippo.shared.dialog

import androidx.compose.runtime.Immutable

@Immutable
internal data class DialogState(
    val process: Process = Process.RELEASE,
    val pendingResult: (() -> Unit)? = null
)

@Immutable
internal enum class Process {
    SHOW,
    DISMISS,
    RELEASE,
}