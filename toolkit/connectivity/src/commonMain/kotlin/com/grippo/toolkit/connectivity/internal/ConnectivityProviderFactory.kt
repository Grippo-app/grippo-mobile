package com.grippo.toolkit.connectivity.internal

import com.grippo.toolkit.connectivity.ConnectivityProvider
import com.grippo.toolkit.context.NativeContext

internal expect fun NativeContext.getConnectivityProvider(): ConnectivityProvider
