package com.grippo.services.apple.auth

public data class AppleAuthException(
    override val message: String,
    override val cause: Throwable? = null,
) : IllegalStateException(message, cause)
