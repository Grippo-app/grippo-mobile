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
        private val singleDialogPresets: Set<KClass<out DialogConfig>> = setOf(
            DialogConfig.ErrorDisplay::class,
        )
    }

    // Show component and bottom-sheet
    private fun show(config: DialogConfig) {
        val isSingleDialog = config::class in singleDialogPresets
        val currentProcess = state.value.process

        val destination = when {
            isSingleDialog -> DialogDirection.Activate(config)
            currentProcess == Process.RELEASE -> DialogDirection.Activate(config)
            else -> DialogDirection.Push(config)
        }

        val newProcess = when (currentProcess) {
            Process.DISMISS, Process.RELEASE -> Process.SHOW(1)
            is Process.SHOW -> Process.SHOW(currentProcess.count + 1)
        }

        log(
            """
            ┌───── Show Dialog ─────
            │ Config      = ${config::class.simpleName}
            │ Type        = ${if (isSingleDialog) "Single" else "Stacked"}
            │ From        = $currentProcess
            │ To          = $newProcess
            │ Destination = ${destination::class.simpleName}
            └───────────────────────
            """.trimIndent()
        )

        update {
            it.copy(
                process = newProcess,
                pendingResult = if (isSingleDialog) null else it.pendingResult,
            )
        }

        navigateTo(destination)
    }

    // Hide bottom-sheet inside of component
    override fun dismiss(pendingResult: (() -> Unit)?) {
        log("🔻 Dismiss called. Saving pendingResult = ${pendingResult != null}")

        if (state.value.process.count > 1) {
            popDialog()
        } else {
            log("🔚 No more dialogs to pop. Moving to DISMISS state")
            update {
                it.copy(
                    process = Process.DISMISS,
                    pendingResult = pendingResult
                )
            }
        }
    }

    // Show previous config from Backstack or release
    override fun pop() {
        log("⬅️ Pop called")

        if (state.value.process.count > 1) {
            popDialog()
        } else {
            log("🛑 Nothing to pop. Ignored.")
            // val config = state.value.stack.current ?: return
            // release(config)
        }
    }

    private fun popDialog() {
        val currentCount = state.value.process.count
        val newCount = currentCount - 1

        log("⬅️ Popping dialog. Stack size: $currentCount → $newCount")

        update {
            it.copy(
                process = Process.SHOW(newCount),
                pendingResult = null
            )
        }

        navigateTo(DialogDirection.Pop)
    }

    // Release dialog component from the graph
    override fun release(config: DialogConfig) {
        log(
            """
            🔓 Releasing dialog
            │ Config     = ${config::class.simpleName}
            │ HasResult  = ${state.value.pendingResult != null}
            │ HasDismiss = ${config.onDismiss != null}
            """.trimIndent()
        )

        state.value.pendingResult?.invoke()
        config.onDismiss?.invoke()

        update { it.copy(process = Process.RELEASE, pendingResult = null) }
        navigateTo(DialogDirection.Dismiss(config))
    }

    private fun log(message: String) {
        println(message)
    }
}