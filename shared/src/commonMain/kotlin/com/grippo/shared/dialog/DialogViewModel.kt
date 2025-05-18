package com.grippo.shared.dialog

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogProvider
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.inject

internal class DialogViewModel :
    BaseViewModel<DialogState, DialogDirection, DialogLoader>(DialogState()), DialogContract {

    private val dialogController by inject<DialogProvider>()

    init {
        dialogController.dialog
            .onEach(::show)
            .safeLaunch()
    }

    // Show component and bottom-sheet
    private fun show(config: DialogConfig) {
        update { it.copy(process = Process.SHOW) }
        navigateTo(DialogDirection.Activate(config))
    }

    // Hide bottom-sheet inside of component
    override fun dismiss() {
        update { it.copy(process = Process.DISMISS) }
    }

    // Release dialog component from the graph
    override fun release() {
        update { it.copy(process = Process.RELEASE) }
        navigateTo(DialogDirection.Dismiss)
    }
}