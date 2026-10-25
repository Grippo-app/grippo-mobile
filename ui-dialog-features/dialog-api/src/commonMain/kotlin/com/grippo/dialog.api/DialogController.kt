package com.grippo.dialog.api

import kotlinx.coroutines.flow.Flow

public interface DialogController {
    public fun open(config: DialogConfig)

    public interface Session {
        public fun push(config: DialogConfig)

        public fun replaceCurrent(config: DialogConfig)

        public fun back()

        public fun close()
    }
}

public interface DialogProvider {
    public val openings: Flow<DialogConfig>
}
