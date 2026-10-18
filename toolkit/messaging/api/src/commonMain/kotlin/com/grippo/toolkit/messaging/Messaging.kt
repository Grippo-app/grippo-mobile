package com.grippo.toolkit.messaging

import com.grippo.toolkit.messaging.state.PushTokenState
import com.grippo.toolkit.messaging.synchronization.PushTokenSyncStatus
import kotlinx.coroutines.flow.StateFlow

public interface Messaging {
    /** Latest token outcome; a newer callback wins against an older in-flight fetch. */
    public val tokenState: StateFlow<PushTokenState>
    /** Backend sync of the latest valid callback, independent of explicit fetches. */
    public val tokenSyncStatus: StateFlow<PushTokenSyncStatus>
    /** Fetches without backend registration; provider exceptions and cancellation propagate. */
    public suspend fun fetchToken(): PushTokenState
}
