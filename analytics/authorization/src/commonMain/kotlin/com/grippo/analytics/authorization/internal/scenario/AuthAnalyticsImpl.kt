package com.grippo.analytics.authorization.internal.scenario

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.internal.state.AuthFlow
import com.grippo.analytics.authorization.internal.state.AuthSessionFlow
import com.grippo.analytics.authorization.models.AuthorizationState
import com.grippo.analytics.authorization.models.EntryPoint
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.FlowId
import org.koin.core.annotation.Single

@Single(binds = [AuthAnalytics::class])
internal class AuthAnalyticsImpl(
    private val analytics: Analytics,
    private val classifier: AuthAnalytics.FailureClassifier,
) : AuthAnalytics {

    override fun authorization(
        entryPoint: EntryPoint,
        flowId: FlowId,
        restored: AuthorizationState?,
    ): AuthAnalytics.Authorization = AuthorizationAnalyticsImpl(
        analytics,
        AuthFlow(restored, entryPoint, analytics, flowId),
        classifier
    )

    override fun session(restored: FlowContext?): AuthAnalytics.Session =
        SessionAnalyticsImpl(analytics, AuthSessionFlow(restored), classifier)
}
