package com.grippo.connectivity

import com.grippo.connectivity.internal.DefaultConnectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

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

public fun Connectivity(
    provider: ConnectivityProvider,
    options: ConnectivityOptions = ConnectivityOptions(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
): Connectivity {
    return DefaultConnectivity(scope, provider, options)
}

public fun Connectivity(
    provider: ConnectivityProvider,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    options: ConnectivityOptions.Builder.() -> Unit,
): Connectivity {
    return Connectivity(provider, ConnectivityOptions.build(options), scope)
}