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
        val stack = state.value.stack
        if (stack.isEmpty()) return

        val last = stack.last()
        val newEntry = last.copy(pendingResult = pendingResult)
        val newStack = stack.dropLast(1) + newEntry

        val process = if (newStack.size == 1) Process.DISMISS else Process.SHOW

        update { it.copy(stack = newStack, process = process) }

        if (newStack.size > 1) {
            pop()
        }
    }

    // Release dialog component from the graph
    override fun onRelease(config: DialogConfig) {
        val stack = state.value.stack
        val last = stack.lastOrNull()

        last?.pendingResult?.invoke()
        config.onDismiss?.invoke()

        update { it.copy(stack = emptyList(), process = Process.RELEASE) }
        navigateTo(DialogDirection.Dismiss)
    }

    private fun show(config: DialogConfig) {
        val stack = state.value.stack
        val newEntry = DialogEntry(config, pendingResult = null)
        val newStack = stack + newEntry

        val destination = when {
            state.value.process == Process.RELEASE -> DialogDirection.Activate(config)
            else -> DialogDirection.Push(config)
        }

        update { it.copy(stack = newStack, process = Process.SHOW) }
        navigateTo(destination)
    }

    private fun pop() {
        val stack = state.value.stack
        if (stack.isEmpty()) return

        val last = stack.last()
        val newStack = stack.dropLast(1)

        update { it.copy(stack = newStack, process = Process.SHOW) }
        navigateTo(DialogDirection.Pop(last.pendingResult))
    }
}