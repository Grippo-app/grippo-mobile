package com.grippo.analytics.authorization.models

import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.NavigationContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Immutable persistence snapshot; the UI stores it without interpreting telemetry state. */
@Serializable
@SerialName("com.grippo.authorization.analytics.internal.AuthFlowState")
public data class AuthorizationState(
    public val flow: FlowContext,
    public val login: NavigationContext,
    public val registration: NavigationContext,
    public val step: Step = Step.Login,
    public val exited: Boolean = false,
)
