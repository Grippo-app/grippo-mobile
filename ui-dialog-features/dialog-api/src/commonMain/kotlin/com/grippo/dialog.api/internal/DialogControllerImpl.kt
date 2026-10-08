package com.grippo.dialog.api.internal

import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.dialog.api.DialogProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.core.annotation.Single

@Single(binds = [DialogController::class, DialogProvider::class])
internal class DialogControllerImpl : DialogController, DialogProvider {

    private val _openings = Channel<DialogConfig>(Channel.CONFLATED)
    override val openings: Flow<DialogConfig> = _openings.receiveAsFlow()

    override fun open(config: DialogConfig) {
        check(_openings.trySend(config).isSuccess) { "Dialog request channel is closed" }
    }
}
