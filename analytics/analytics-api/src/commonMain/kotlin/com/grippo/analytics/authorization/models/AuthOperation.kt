package com.grippo.analytics.authorization.models

/** Closed operation descriptions; decorators preserve the caller’s business result. */
public sealed interface AuthOperation {
    public sealed interface Attempt : AuthOperation

    public data class Login(public val method: Method) : Attempt

    public data object Register : Attempt

    public data object SessionCheck : AuthOperation
}
