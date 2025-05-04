package com.grippo.shared.dialog

import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogController
import com.grippo.dialog.api.DialogEvent
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.inject

internal class DialogViewModel :
    BaseViewModel<DialogState, DialogDirection, DialogLoader>(DialogState), DialogContract {

    private val dialogController by inject<DialogController>()

    init {
        dialogController.dialog
            .onEach {
                when (it) {
                    DialogEvent.Dismiss -> dialog.dismiss()
                    is DialogEvent.Show -> dialog.activate(it.config)
                }
            }.launchIn(coroutineScope)
    }

    override fun dismiss() {
        dialog.dismiss()
    }
}