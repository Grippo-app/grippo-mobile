package com.grippo.services.google.auth

public class GoogleAuthException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
