package com.grippo.toolkit.connectivity.internal

import com.grippo.toolkit.connectivity.ConnectivityProvider
import com.grippo.toolkit.context.NativeContext

internal actual fun NativeContext.getConnectivityProvider(): ConnectivityProvider {
    return AndroidConnectivityProvider(this.context)
}
