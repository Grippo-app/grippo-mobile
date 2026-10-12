package com.grippo.analytics.authorization.models

/** Complete business operations; telemetry never determines their results or recovery. */
public sealed interface AuthOperation {
    public data class Login(public val method: Method) : AuthOperation
    public data object Register : AuthOperation
    public data object SessionCheck : AuthOperation
    public data object CreateProfile : AuthOperation
    public data object Logout : AuthOperation
}
