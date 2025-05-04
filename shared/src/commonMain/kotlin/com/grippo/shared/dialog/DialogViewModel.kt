package com.grippo.shared.dialog

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogController
import com.grippo.dialog.api.DialogEvent
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.inject

internal class DialogViewModel :
    BaseViewModel<DialogState, DialogDirection, DialogLoader>(DialogState()), DialogContract {

    private val dialogController by inject<DialogController>()

    init {
        dialogController.dialog
            .onEach { event -> update { it.copy(event = event) } }
            .launchIn(coroutineScope)
    }

    override fun dismiss() {
        update { it.copy(event = DialogEvent.Dismiss) }
    }
}