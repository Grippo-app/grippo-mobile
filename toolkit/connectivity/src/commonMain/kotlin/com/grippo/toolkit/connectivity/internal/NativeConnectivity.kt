package com.grippo.toolkit.connectivity.internal

import com.grippo.toolkit.connectivity.Connectivity
import com.grippo.toolkit.connectivity.ConnectivityOptions
import com.grippo.toolkit.connectivity.ConnectivityProvider
import com.grippo.toolkit.context.NativeContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

internal fun NativeContext.createConnectivity(
    provider: ConnectivityProvider = getConnectivityProvider(),
    options: ConnectivityOptions = ConnectivityOptions(autoStart = true),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
): Connectivity {
    return DefaultConnectivity(scope, provider, options)
}
