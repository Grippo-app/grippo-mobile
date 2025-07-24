package com.grippo.shared.dialog

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogProvider
import kotlinx.coroutines.flow.onEach
import kotlin.reflect.KClass

internal class DialogViewModel(
    dialogProvider: DialogProvider
) : BaseViewModel<DialogState, DialogDirection, DialogLoader>(DialogState()), DialogContract {

    init {
        dialogProvider.dialog
            .onEach(::show)
            .safeLaunch()
    }

    companion object {
        private val soloDialogPresets: Set<KClass<out DialogConfig>> = setOf(
            DialogConfig.ErrorDisplay::class,
        )
    }

    // Show component and bottom-sheet
    private fun show(config: DialogConfig) {
        val isSoloDialog = config::class in soloDialogPresets

        val newStack = when {
            isSoloDialog -> DialogStack(listOf(config))
            state.value.process == Process.RELEASE -> DialogStack(listOf(config))
            else -> state.value.stack.push(config)
        }

        update {
            it.copy(
                process = Process.SHOW,
                stack = newStack,
                pendingResult = if (isSoloDialog) null else it.pendingResult
            )
        }

        navigateTo(DialogDirection.Activate(config))
    }

    // Hide bottom-sheet inside of component
    override fun dismiss(pendingResult: (() -> Unit)?) {
        popOrElse {
            update { it.copy(process = Process.DISMISS, pendingResult = pendingResult) }
        }
    }

    // Show previous config from Backstack or release
    override fun back() {
        popOrElse {
            val config = state.value.stack.current ?: return@popOrElse
            release(config)
        }
    }

    private fun popOrElse(lambda: () -> Unit) {
        val stack = state.value.stack

        if (stack.stack.size > 1) {
            val newStack = stack.pop()
            val prevConfig = newStack.current ?: return

            update { it.copy(process = Process.SHOW, stack = newStack, pendingResult = null) }

            navigateTo(DialogDirection.Activate(prevConfig))
        } else {
            lambda.invoke()
        }
    }

    // Release dialog component from the graph
    override fun release(config: DialogConfig) {
        state.value.pendingResult?.invoke()
        config.onDismiss?.invoke()
        update { it.copy(process = Process.RELEASE, pendingResult = null) }
        navigateTo(DialogDirection.Dismiss(config))
    }
}