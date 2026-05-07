package com.grippo.shared.dialog

import androidx.compose.runtime.Immutable
import com.grippo.dialog.api.DialogConfig
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class DialogState(
    val stack: ImmutableList<DialogEntry> = persistentListOf(),
    val phase: SheetPhase = SheetPhase.Released,
    val pending: DialogConfig? = null,
) {
    val sessionConfig: DialogConfig? = if (phase == SheetPhase.Released) {
        null
    } else {
        stack.firstOrNull()?.config
    }

    val innerConfigs: List<DialogConfig> = if (stack.size <= 1) {
        emptyList()
    } else {
        stack.drop(1).map { it.config }
    }
}

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