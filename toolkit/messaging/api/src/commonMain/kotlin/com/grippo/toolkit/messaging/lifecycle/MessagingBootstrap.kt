package com.grippo.toolkit.messaging.lifecycle

import com.grippo.toolkit.messaging.transport.PushTokenProvider

public interface MessagingBootstrap {
    public fun attachTokenProvider(tokenProvider: PushTokenProvider)

    public fun onTokenChanged(rawToken: String)
}
