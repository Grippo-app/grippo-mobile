package com.grippo.dialog.api.internal

import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.dialog.api.DialogEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal class DialogControllerImpl : DialogController {

    private val _dialog = Channel<DialogEvent>(Channel.CONFLATED)
    override val dialog: Flow<DialogEvent> = _dialog.receiveAsFlow()

    override fun show(config: DialogConfig) {
        _dialog.trySend(DialogEvent.Show(config))
    }

    override fun dismiss() {
        _dialog.trySend(DialogEvent.Dismiss)
    }
}