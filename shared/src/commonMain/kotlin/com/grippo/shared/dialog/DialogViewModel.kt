package com.grippo.shared.dialog

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.inject

internal class DialogViewModel :
    BaseViewModel<DialogState, DialogDirection, DialogLoader>(DialogState), DialogContract {

    private val dialogController by inject<DialogController>()

    init {
        dialogController.dialog
            .onEach { navigateTo(DialogDirection.Show(it)) }
            .launchIn(coroutineScope)
    }

    fun dismiss() {
        navigateTo(DialogDirection.Dismiss)
    }
}