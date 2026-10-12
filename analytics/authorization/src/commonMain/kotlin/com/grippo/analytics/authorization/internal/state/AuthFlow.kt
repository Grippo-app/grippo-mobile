package com.grippo.analytics.authorization.internal.state

import com.grippo.analytics.authorization.internal.events.AuthFlowEvent
import com.grippo.analytics.authorization.internal.schema.AuthEntryPoints
import com.grippo.analytics.authorization.internal.schema.AuthScreen
import com.grippo.analytics.authorization.models.AuthorizationState
import com.grippo.analytics.authorization.models.ExitReason
import com.grippo.analytics.authorization.models.Step
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.FlowId
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.analytics.authorization.models.EntryPoint as AuthEntryPoint

/** Navigation-owned attribution, saved with the component instead of global last-screen state. */
internal class AuthFlow(
    restored: AuthorizationState?,
    entryPoint: AuthEntryPoint,
    private val analytics: Analytics,
    flowId: FlowId,
) {
    private var state: AuthorizationState
    val context: FlowContext get() = state.flow

    init {
        require(restored == null || restored.flow.id == flowId) {
            "Restored authorization flow must match its route"
        }

        state = restored?.copy(
            login = restored.login.copy(reason = NavigationReason.Restore),
            registration = restored.registration.copy(reason = NavigationReason.Restore),
        ) ?: run {
            val origin = when (entryPoint) {
                AuthEntryPoint.LaunchWithoutSession,
                AuthEntryPoint.SessionCheckFailed -> AuthScreen.Splash

                AuthEntryPoint.ProfileSetupReturn -> AuthScreen.ProfileCreationUser
                AuthEntryPoint.TokenAbsent,
                AuthEntryPoint.RestoredScenario -> null
            }
            AuthorizationState(
                flow = FlowContext(flowId, AuthEntryPoints.from(entryPoint), origin),
                login = NavigationContext(
                    AuthScreen.Login, origin,
                    when (entryPoint) {
                        AuthEntryPoint.ProfileSetupReturn -> NavigationReason.Back
                        AuthEntryPoint.RestoredScenario -> NavigationReason.Restore
                        else -> NavigationReason.Initial
                    },
                ),
                registration = NavigationContext(
                    AuthScreen.Registration,
                    AuthScreen.Login,
                    NavigationReason.Action
                ),
            )
        }
        if (restored == null) {
            report(AuthFlowEvent.FlowStarted)
        }
    }

    fun navigation(step: Step): NavigationContext {
        val navigation = when (step) {
            Step.Login -> state.login
            Step.Registration -> state.registration
        }
        val next = navigation.copy(referrer = navigation.screen, reason = NavigationReason.Restore)
        state = when (step) {
            Step.Login -> state.copy(login = next, step = step)
            Step.Registration -> state.copy(registration = next, step = step)
        }
        return navigation
    }

    fun openRegistration() {
        if (state.exited || state.step == Step.Registration) return
        state = state.copy(
            registration = NavigationContext(
                AuthScreen.Registration,
                AuthScreen.Login,
                NavigationReason.Action
            ),
            step = Step.Registration,
        )
        report(
            AuthFlowEvent.StepChanged(
                Step.Login,
                Step.Registration,
                NavigationReason.Action
            )
        )
    }

    fun backToLogin() {
        if (state.exited || state.step == Step.Login) return
        state = state.copy(
            login = NavigationContext(
                AuthScreen.Login,
                AuthScreen.Registration,
                NavigationReason.Back
            ),
            step = Step.Login,
        )
        report(
            AuthFlowEvent.StepChanged(
                Step.Registration,
                Step.Login,
                NavigationReason.Back
            )
        )
    }

    fun exit(reason: ExitReason) {
        if (state.exited) return
        state = state.copy(exited = true)
        report(AuthFlowEvent.FlowExited(reason, state.step))
    }

    fun snapshot(): AuthorizationState = state

    private fun report(event: AnalyticsEvent) {
        try {
            analytics.track(event, context)
        } catch (_: Exception) {
            // Telemetry must not prevent construction or business navigation.
        }
    }
}
