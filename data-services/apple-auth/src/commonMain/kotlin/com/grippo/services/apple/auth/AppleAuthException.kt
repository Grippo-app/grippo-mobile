package com.grippo.services.apple.auth

public data class AppleAuthException(
    override val message: String,
    override val cause: Throwable? = null,
    public val reason: AppleAuthFailureReason = AppleAuthFailureReason.ProviderFailure,
) : IllegalStateException(message, cause)
