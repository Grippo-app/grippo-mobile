package com.grippo.toolkit.connectivity

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// https://github.com/jordond/connectivity?tab=readme-ov-file#features
public interface Connectivity {

    public val statusUpdates: SharedFlow<Status>

    public val monitoring: StateFlow<Boolean>

    public val isMonitoring: Boolean
        get() = monitoring.value

    public suspend fun status(): Status
    public fun start()
    public fun stop()

    public sealed interface Status {
        public val isConnected: Boolean
            get() = this is Connected
        public val isMetered: Boolean
            get() = this is Connected && metered
        public val isDisconnected: Boolean
            get() = this is Disconnected

        public data class Connected(public val metered: Boolean) : Status
        public data object Disconnected : Status
    }
}
