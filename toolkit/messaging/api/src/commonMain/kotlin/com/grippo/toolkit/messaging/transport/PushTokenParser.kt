package com.grippo.toolkit.messaging.transport

import com.grippo.toolkit.messaging.state.PushTokenFailure
import com.grippo.toolkit.messaging.state.PushTokenState
import com.grippo.toolkit.messaging.token.PushToken

public object PushTokenParser {
    public fun parse(rawToken: String): PushTokenState = try {
        PushTokenState.Available(PushToken(rawToken))
    } catch (_: IllegalArgumentException) {
        PushTokenState.Failed(PushTokenFailure.InvalidToken)
    }
}
