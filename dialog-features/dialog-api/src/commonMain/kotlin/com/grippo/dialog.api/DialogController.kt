package com.grippo.dialog.api

import kotlinx.coroutines.flow.Flow

public interface DialogController {
    public val dialog: Flow<DialogConfig>
    public fun show(config: DialogConfig)
}