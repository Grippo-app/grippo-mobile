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

    private fun show(config: DialogConfig) {
        val stack = state.value.stack
        val newEntry = DialogEntry(config, pendingResult = null)
        val newStack = stack + newEntry

        val destination = when {
            state.value.process == Process.RELEASE -> DialogDirection.Activate(config)
            else -> DialogDirection.Push(config)
        }

        logBlock("Show Dialog") {
            log("📥 Received config: ${config::class.simpleName}")
            log("📦 Stack size: ${stack.size} → ${newStack.size}")
            log("📌 Destination: ${destination::class.simpleName}")
            log("🧾 Full stack (top to bottom):")
            newStack.reversed().forEachIndexed { index, entry ->
                log("   ${index + 1}) ${entry.config::class.simpleName} → pendingResult: ${entry.pendingResult != null}")
            }
        }

        update { it.copy(stack = newStack, process = Process.SHOW) }
        navigateTo(destination)
    }

    override fun dismiss(pendingResult: (() -> Unit)?) {
        val stack = state.value.stack

        logBlock("Dismiss") {
            log("📦 Stack size: ${stack.size}")
            log("⏱ Should wait for release: ${stack.size == 1}")
            log("💾 Saving pendingResult: ${pendingResult != null}")
            log("🧾 Full stack (top to bottom):")
            stack.reversed().forEachIndexed { index, entry ->
                log("   ${index + 1}) ${entry.config::class.simpleName} → pendingResult: ${entry.pendingResult != null}")
            }
        }

        if (stack.isEmpty()) return

        val last = stack.last()
        val newEntry = last.copy(pendingResult = pendingResult)
        val newStack = stack.dropLast(1) + newEntry

        val process = if (newStack.size == 1) Process.DISMISS else Process.SHOW

        update { it.copy(stack = newStack, process = process) }

        if (newStack.size == 1) {
            log("✅ Updated process to DISMISS with pendingResult stored")
        } else {
            popDialog()
        }
    }

    override fun pop() {
        val stack = state.value.stack

        logBlock("Pop") {
            log("📦 Stack size: ${stack.size}")
        }

        if (stack.size > 1) {
            popDialog()
        } else {
            log("🛑 Nothing to pop — stack size is 1")
        }
    }

    private fun popDialog() {
        val stack = state.value.stack
        if (stack.isEmpty()) return

        val last = stack.last()
        val newStack = stack.dropLast(1)

        logBlock("Pop Dialog") {
            log("📤 Triggered pendingResult from: ${last.config::class.simpleName}")
            log("📦 Stack size: ${stack.size} → ${newStack.size}")
            log("📌 pendingResult for removed entry: ${last.pendingResult != null}")
            log("🧾 Full stack (top to bottom):")
            stack.reversed().forEachIndexed { index, entry ->
                log("   ${index + 1}) ${entry.config::class.simpleName} → pendingResult: ${entry.pendingResult != null}")
            }
        }

        update { it.copy(stack = newStack, process = Process.SHOW) }
        navigateTo(DialogDirection.Pop(last.pendingResult))
    }

    // Release dialog component from the graph
    override fun release(config: DialogConfig) {
        val stack = state.value.stack
        val last = stack.lastOrNull()

        logBlock("Release Dialog") {
            log("📦 Stack size: ${stack.size}")
            log("🔚 Releasing config: ${config::class.simpleName}")
            log("💥 Triggering pendingResult: ${last?.pendingResult != null}")
            log("📭 onDismiss present: ${config.onDismiss != null}")
            log("🧾 Full stack (top to bottom):")
            stack.reversed().forEachIndexed { index, entry ->
                log("   ${index + 1}) ${entry.config::class.simpleName} → pendingResult: ${entry.pendingResult != null}")
            }
        }

        last?.pendingResult?.invoke()
        config.onDismiss?.invoke()

        update { it.copy(stack = emptyList(), process = Process.RELEASE) }
        navigateTo(DialogDirection.Dismiss)
    }

    private fun log(message: String) {
        println("│ $message")
    }

    private inline fun logBlock(title: String, block: () -> Unit) {
        println("┌───── $title ─────")
        block()
        println("└──────────────────────")
    }
}