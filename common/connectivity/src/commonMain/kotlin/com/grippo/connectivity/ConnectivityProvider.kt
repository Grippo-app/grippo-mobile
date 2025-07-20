package com.grippo.connectivity

import com.grippo.platform.core.NativeContext
import kotlinx.coroutines.flow.Flow

internal expect fun NativeContext.getConnectivityProvider(): ConnectivityProvider

public interface ConnectivityProvider {
    public fun monitor(): Flow<Connectivity.Status>
}

public fun ConnectivityProvider(
    flow: Flow<Connectivity.Status>,
): ConnectivityProvider = object : ConnectivityProvider {
    override fun monitor(): Flow<Connectivity.Status> = flow
}

public fun Flow<Connectivity.Status>.asProvider(): ConnectivityProvider = ConnectivityProvider(this)