package com.grippo.shared.dialog

import com.grippo.core.foundation.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

internal class DialogViewModel(
    dialogProvider: DialogProvider
) : BaseViewModel<DialogState, DialogDirection, DialogLoader>(DialogState()), DialogContract {

    init {
        dialogProvider.dialog
            .onEach(::show)
            .safeLaunch(processing = Processing.Infinity)
    }

    override fun onDismiss(pendingResult: (() -> Unit)?) {
        val stack = state.value.stack
        if (stack.isEmpty()) return

        if (stack.size == 1) {
            // Outermost dialog: hand off to the sheet hide animation.
            // pendingResult fires from onRelease once the animation completes.
            val withPending = stack.last().copy(pendingResult = pendingResult)
            update {
                it.copy(
                    stack = persistentListOf(withPending),
                    phase = SheetPhase.Dismissing,
                )
            }
        } else {
            // In-sheet pop: bottom sheet stays on screen, only inner content swaps.
            // The reconciler will pop Decompose's stack to match. pendingResult fires
            // immediately so a chained show() from it lands in the same animation cycle.
            update { it.copy(stack = stack.dropLast(1).toPersistentList()) }
            pendingResult?.invoke()
        }
    }

    override fun onClose() {
        if (state.value.stack.isEmpty()) return
        // Stack stays — sheet still needs content while animating out.
        // onRelease clears it after the hide animation completes.
        update { it.copy(phase = SheetPhase.Dismissing) }
    }

    override fun onRelease(config: DialogConfig) {
        val current = state.value
        if (current.phase == SheetPhase.Released) return

        // These callbacks may synchronously call show(...), which routes into [show]
        // and lands in [DialogState.pending] (phase is still Dismissing here).
        current.stack.lastOrNull()?.pendingResult?.invoke()
        config.onDismiss?.invoke()

        val nextShow = state.value.pending

        if (nextShow != null) {
            update {
                it.copy(
                    stack = persistentListOf(DialogEntry(nextShow, pendingResult = null)),
                    phase = SheetPhase.Present,
                    pending = null,
                )
            }
        } else {
            update {
                it.copy(
                    stack = persistentListOf(),
                    phase = SheetPhase.Released,
                    pending = null,
                )
            }
        }
    }

    private fun show(config: DialogConfig) {
        val current = state.value

        // Skip exact duplicates that are still visible. Entries marked with a
        // pendingResult are about to be removed by onRelease, so chaining onto the
        // same key after a confirm-and-replace is allowed.
        val isDuplicate = current.stack.any {
            it.config.matches(config) && it.pendingResult == null
        } || current.pending?.matches(config) == true
        if (isDuplicate) return

        if (current.phase == SheetPhase.Dismissing) {
            // Previous dialog is still animating out; defer to onRelease.
            update { it.copy(pending = config) }
            return
        }

        update {
            it.copy(
                stack = (it.stack + DialogEntry(config, pendingResult = null)).toPersistentList(),
                phase = SheetPhase.Present,
            )
        }
    }

    private fun DialogConfig.matches(other: DialogConfig): Boolean =
        this::class == other::class && this.key == other.key
}
