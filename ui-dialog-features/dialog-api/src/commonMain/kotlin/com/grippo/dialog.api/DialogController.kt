package com.grippo.dialog.api

import kotlinx.coroutines.flow.Flow

public interface DialogController {
    /** Opens a new sheet session. An existing session closes before this one opens. */
    public fun open(config: DialogConfig)

    /**
     * Supplied by the dialog host, bound to the originating session and step.
     * Call on the UI thread. Calls from inactive or removed steps are ignored.
     */
    public interface Session {
        /** Opens the next step, retaining the current step for Back. */
        public fun push(config: DialogConfig)

        /** Replaces the current step, keeping the sheet and earlier steps. */
        public fun replaceCurrent(config: DialogConfig)

        /** Returns to the previous step, or closes the sheet at the root. */
        public fun back()

        /** Closes the entire sheet session. */
        public fun close()
    }
}

public interface DialogProvider {
    public val openings: Flow<DialogConfig>
}
