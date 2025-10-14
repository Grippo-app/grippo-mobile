package com.grippo.connectivity

import com.grippo.platform.context.NativeContext

internal actual fun NativeContext.getConnectivityProvider(): ConnectivityProvider {
    return AppleConnectivityProvider()
}