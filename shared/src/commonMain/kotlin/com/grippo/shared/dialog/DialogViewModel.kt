package com.grippo.shared.dialog

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogProvider
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.inject

internal class DialogViewModel :
    BaseViewModel<DialogState, DialogDirection, DialogLoader>(DialogState()), DialogContract {

    private val dialogController by inject<DialogProvider>()

    init {
        dialogController.dialog
            .onEach(::show)
            .launchIn(coroutineScope)
    }

    private fun show(config: DialogConfig) {
        update { it.copy(process = Process.SHOW) }
        navigateTo(DialogDirection.Activate(config))
    }

    override fun release() {
        update { it.copy(process = Process.RELEASE) }
        navigateTo(DialogDirection.Dismiss)
    }

    override fun dismiss() {
        update { it.copy(process = Process.DISMISS) }
    }
}