package com.grippo.analytics.authorization.internal.scenario

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.internal.operation.AuthSessionCheckEvents
import com.grippo.analytics.authorization.internal.state.AuthSessionFlow
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.analytics.operation.trackOperation

internal class SessionAnalyticsImpl(
    private val analytics: Analytics,
    private val flow: AuthSessionFlow,
    private val classifier: AuthAnalytics.FailureClassifier,
) : AuthAnalytics.Session {
    override fun presentScreen(): ScreenPresentation =
        analytics.presentScreen(flow.navigation(), flow.context)

    override suspend fun <T> trackOperation(
        operation: AuthOperation.SessionCheck,
        block: suspend () -> T
    ): T =
        analytics.trackOperation(AuthSessionCheckEvents<T>(classifier), flow.context, block)

    override fun save(): FlowContext = flow.context
}
