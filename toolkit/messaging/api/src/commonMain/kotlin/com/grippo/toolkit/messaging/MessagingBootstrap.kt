package com.grippo.toolkit.messaging

/** App startup contract. Feature code injects the consumer API instead. */
public interface MessagingBootstrap {
    /** Connects exactly once; duplicate connection or connection after close fails. */
    public fun attachSink(messagingSink: MessagingSink)

    /** Validates an untrusted SDK callback before publishing it. */
    public fun onRegistrationToken(value: String)
}
