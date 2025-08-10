package com.grippo.shared.dialog

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogProvider
import kotlinx.coroutines.flow.onEach

internal class DialogViewModel(
    dialogProvider: DialogProvider
) : BaseViewModel<DialogState, DialogDirection, DialogLoader>(DialogState()), DialogContract {

    init {
        dialogProvider.dialog
            .onEach(::show)
            .safeLaunch()
    }

    override fun onDismiss(pendingResult: (() -> Unit)?) {
        val oldStack = state.value.stack
        if (oldStack.isEmpty()) return

        val updatedTop = oldStack.last().copy(pendingResult = pendingResult)
        val newStack = oldStack.dropLast(1) + updatedTop
        val nextPhase = if (newStack.size == 1) SheetPhase.DISMISSING else SheetPhase.PRESENT

        update { it.copy(stack = newStack, phase = nextPhase) }

        if (newStack.size > 1) pop()
    }

    override fun onClose() {
        if (state.value.stack.isEmpty()) return
        update { it.copy(stack = emptyList(), phase = SheetPhase.DISMISSING) }
    }

    override fun onRelease(config: DialogConfig) {
        val current = state.value
        if (current.phase == SheetPhase.RELEASED) return

        current.stack.lastOrNull()?.pendingResult?.invoke()
        config.onDismiss?.invoke()

        update { prev -> prev.copy(stack = emptyList(), phase = SheetPhase.RELEASED) }
        navigateTo(DialogDirection.Dismiss)
    }

    private fun show(config: DialogConfig) {
        val stack = state.value.stack
        val newStack = stack + DialogEntry(config, pendingResult = null)

        val destination =
            if (state.value.phase == SheetPhase.RELEASED) DialogDirection.Activate(config)
            else DialogDirection.Push(config)

        update { it.copy(stack = newStack, phase = SheetPhase.PRESENT) }
        navigateTo(destination)
    }

    private fun pop() {
        val stack = state.value.stack
        if (stack.isEmpty()) return

        val last = stack.last()
        val newStack = stack.dropLast(1)

        update { it.copy(stack = newStack, phase = SheetPhase.PRESENT) }
        navigateTo(DialogDirection.Pop(last.pendingResult))
    }
}