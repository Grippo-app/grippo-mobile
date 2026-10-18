package com.grippo.dialog.api

import kotlinx.coroutines.flow.Flow

public interface DialogController {
    /**
     * Opens a new sheet session after the current one closes. The latest pending
     * ordinary opening wins. Errors are retained in arrival order ahead of the
     * pending ordinary opening. An active error waits for dismissal.
     */
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
