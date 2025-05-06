package com.grippo.dialog.api

import kotlinx.coroutines.flow.Flow

public interface DialogController {
    public fun show(config: DialogConfig)
}

public interface DialogProvider {
    public val dialog: Flow<DialogConfig>
}