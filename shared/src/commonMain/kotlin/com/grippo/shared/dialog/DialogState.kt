package com.grippo.shared.dialog

import androidx.compose.runtime.Immutable
import com.grippo.dialog.api.DialogConfig

@Immutable
internal data class DialogState(
    val stack: List<DialogEntry> = emptyList(),
    val phase: SheetPhase = SheetPhase.RELEASED,
)

@Immutable
internal data class DialogEntry(
    val config: DialogConfig,
    val pendingResult: (() -> Unit)? = null
)

@Immutable
internal sealed class SheetPhase {
    @Immutable
    data object PRESENT : SheetPhase()

    @Immutable
    data object DISMISSING : SheetPhase()

    @Immutable
    data object RELEASED : SheetPhase()
}