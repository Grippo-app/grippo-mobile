package com.grippo.analytics.authorization.models

/** Observed scenario transitions; reporting does not perform UI navigation. */
public sealed interface AuthTransition {
    public data object OpenRegistration : AuthTransition

    public data object BackToLogin : AuthTransition

    public data class Exit(public val reason: ExitReason) : AuthTransition
}
