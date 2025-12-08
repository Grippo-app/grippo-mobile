package com.grippo.services.google.auth

public data class GoogleAuthException(
    override val message: String,
    override val cause: Throwable? = null,
) : IllegalStateException(message, cause)
