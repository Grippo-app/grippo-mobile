package com.grippo.shared.dialog

import androidx.compose.runtime.Immutable

@Immutable
internal data class DialogState(
    val process: Process = Process.RELEASE,
    val pendingResult: (() -> Unit)? = null,
)

@Immutable
internal sealed class Process(open val count: Int) {
    @Immutable
    data class SHOW(override val count: Int) : Process(count)

    @Immutable
    data object DISMISS : Process(0)

    @Immutable
    data object RELEASE : Process(0)
}