package com.grippo.toolkit.messaging.lifecycle

import com.grippo.toolkit.messaging.transport.PushTokenProvider

/** App startup contract. Feature code injects the consumer API instead. */
public interface MessagingBootstrap {
    /** Connects exactly once; duplicate connection or connection after close fails. */
    public fun attachTokenProvider(tokenProvider: PushTokenProvider)

    /** Validates an untrusted SDK callback before publishing it. */
    public fun onTokenChanged(rawToken: String)
}
