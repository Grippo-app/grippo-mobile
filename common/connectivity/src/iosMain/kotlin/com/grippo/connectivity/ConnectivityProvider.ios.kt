package com.grippo.connectivity

import com.grippo.toolkit.context.NativeContext

internal actual fun NativeContext.getConnectivityProvider(): ConnectivityProvider {
    return AppleConnectivityProvider()
}