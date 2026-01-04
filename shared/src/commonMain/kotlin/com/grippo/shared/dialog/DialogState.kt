package com.grippo.shared.dialog

import androidx.compose.runtime.Immutable
import com.grippo.dialog.api.DialogConfig

@Immutable
internal data class DialogState(
    val stack: List<DialogEntry> = emptyList(),
    val phase: SheetPhase = SheetPhase.Released,
)

@Immutable
internal data class DialogEntry(
    val config: DialogConfig,
    val pendingResult: (() -> Unit)? = null
)

@Immutable
internal sealed class SheetPhase {
    @Immutable
    data object Present : SheetPhase()

    @Immutable
    data object Dismissing : SheetPhase()

    @Immutable
    data object Released : SheetPhase()
}