package com.grippo.shared.dialog

import androidx.compose.runtime.Immutable
import com.grippo.dialog.api.DialogConfig

internal data class DialogState(
    val stack: List<DialogEntry> = emptyList(),
    val process: Process = Process.RELEASE,
)

@Immutable
internal data class DialogEntry(
    val config: DialogConfig,
    val pendingResult: (() -> Unit)? = null
)

@Immutable
internal sealed class Process {
    @Immutable
    data object SHOW : Process()

    @Immutable
    data object DISMISS : Process()

    @Immutable
    data object RELEASE : Process()
}