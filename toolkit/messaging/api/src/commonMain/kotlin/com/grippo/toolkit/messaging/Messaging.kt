package com.grippo.toolkit.messaging

import com.grippo.toolkit.messaging.state.PushTokenState
import com.grippo.toolkit.messaging.synchronization.PushTokenSyncStatus
import kotlinx.coroutines.flow.StateFlow

public interface Messaging {
    public val tokenState: StateFlow<PushTokenState>
    public val tokenSyncStatus: StateFlow<PushTokenSyncStatus>
    public suspend fun fetchToken(): PushTokenState
}
