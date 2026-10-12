package com.grippo.analytics.authorization.internal.scenario

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.internal.models.AuthAction
import com.grippo.analytics.authorization.internal.operation.AuthAttemptEvents
import com.grippo.analytics.authorization.internal.state.AuthFlow
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.AuthTransition
import com.grippo.analytics.authorization.models.AuthorizationState
import com.grippo.analytics.authorization.models.Method
import com.grippo.analytics.authorization.models.Step
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.analytics.operation.trackOperation

internal class AuthorizationAnalyticsImpl(
    private val analytics: Analytics,
    private val flow: AuthFlow,
    private val classifier: AuthAnalytics.FailureClassifier,
) : AuthAnalytics.Authorization {
    override fun presentScreen(step: Step): ScreenPresentation =
        analytics.presentScreen(flow.navigation(step), flow.context)

    override fun trackTransition(transition: AuthTransition) {
        when (transition) {
            AuthTransition.OpenRegistration -> flow.openRegistration()
            AuthTransition.BackToLogin -> flow.backToLogin()
            is AuthTransition.Exit -> flow.exit(transition.reason)
        }
    }

    override suspend fun <T> trackOperation(
        operation: AuthOperation.Attempt,
        block: suspend () -> T
    ): T {
        val events = when (operation) {
            is AuthOperation.Login -> AuthAttemptEvents<T>(
                operation.method,
                AuthAction.Login,
                classifier
            )

            AuthOperation.Register -> AuthAttemptEvents<T>(
                Method.Email,
                AuthAction.Register,
                classifier
            )
        }
        return analytics.trackOperation(events, flow.context, block)
    }

    override fun save(): AuthorizationState = flow.snapshot()
}
