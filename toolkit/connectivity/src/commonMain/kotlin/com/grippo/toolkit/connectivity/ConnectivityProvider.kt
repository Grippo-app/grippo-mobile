package com.grippo.toolkit.connectivity

import kotlinx.coroutines.flow.Flow

public interface ConnectivityProvider {
    public fun monitor(): Flow<Connectivity.Status>
}
