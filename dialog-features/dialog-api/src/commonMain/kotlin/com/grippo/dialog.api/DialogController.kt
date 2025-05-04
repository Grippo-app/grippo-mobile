package com.grippo.dialog.api

import kotlinx.coroutines.flow.Flow

public interface DialogController {
    public val dialog: Flow<DialogEvent>

    public fun show(config: DialogConfig)
    public fun dismiss()
}