package com.grippo.dialog.api.internal

import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.dialog.api.DialogProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal class DialogControllerImpl : DialogController, DialogProvider {

    private val _dialog = Channel<DialogConfig>(Channel.CONFLATED)
    override val dialog: Flow<DialogConfig> = _dialog.receiveAsFlow()

    override fun show(config: DialogConfig) {
        _dialog.trySend(config)
    }
}