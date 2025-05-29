package com.grippo.shared.dialog

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogProvider
import com.grippo.logger.AppLogger
import kotlinx.coroutines.flow.onEach

internal class DialogViewModel(
    dialogProvider: DialogProvider
) : BaseViewModel<DialogState, DialogDirection, DialogLoader>(DialogState()), DialogContract {

    init {
        dialogProvider.dialog
            .onEach(::show)
            .safeLaunch()
    }

    // Show component and bottom-sheet
    private fun show(config: DialogConfig) {
        if (state.value.process != Process.RELEASE) {
            AppLogger.warning("🚫 Dialog already active. Skipping: $config")
            return
        }

        update { it.copy(process = Process.SHOW) }
        navigateTo(DialogDirection.Activate(config))
    }

    // Hide bottom-sheet inside of component
    override fun dismiss(pendingResult: (() -> Unit)?) {
        update { it.copy(process = Process.DISMISS, pendingResult = pendingResult) }
    }

    // Release dialog component from the graph
    override fun release(config: DialogConfig) {
        state.value.pendingResult?.invoke()
        config.onDismiss?.invoke()
        update { it.copy(process = Process.RELEASE, pendingResult = null) }
        navigateTo(DialogDirection.Dismiss(config))
    }
}