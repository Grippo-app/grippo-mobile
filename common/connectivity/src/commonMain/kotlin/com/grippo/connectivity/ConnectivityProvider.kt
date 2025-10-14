package com.grippo.connectivity

import com.grippo.platform.context.NativeContext
import kotlinx.coroutines.flow.Flow

internal expect fun NativeContext.getConnectivityProvider(): ConnectivityProvider

public interface ConnectivityProvider {
    public fun monitor(): Flow<Connectivity.Status>
}
