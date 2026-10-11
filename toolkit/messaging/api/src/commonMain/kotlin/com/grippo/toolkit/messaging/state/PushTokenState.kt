package com.grippo.toolkit.messaging.state

import com.grippo.toolkit.messaging.token.PushToken

public sealed interface PushTokenState {
    public class Available(public val token: PushToken) : PushTokenState
    public class Unavailable(public val reason: PushTokenUnavailableReason) : PushTokenState
    public class Failed(public val reason: PushTokenFailure) : PushTokenState
}
