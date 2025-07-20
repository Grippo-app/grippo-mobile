package com.grippo.connectivity

import com.grippo.platform.core.NativeContext

internal actual fun NativeContext.getConnectivityProvider(): ConnectivityProvider {
    return AppleConnectivityProvider()
}