package com.grippo.dialog.api.internal

import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.dialog.api.DialogProvider
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.core.annotation.Single

@Single(binds = [DialogController::class, DialogProvider::class])
internal class DialogControllerImpl : DialogController, DialogProvider {

    private val _dialog = MutableSharedFlow<DialogConfig>(
        replay = 1,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val dialog: Flow<DialogConfig> = _dialog

    override fun show(config: DialogConfig) {
        _dialog.tryEmit(config)
    }
}