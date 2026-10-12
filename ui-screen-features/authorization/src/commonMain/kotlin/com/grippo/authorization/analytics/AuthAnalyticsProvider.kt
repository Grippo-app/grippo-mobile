package com.grippo.authorization.analytics

import com.arkivanov.decompose.ComponentContext
import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.models.AuthorizationState
import com.grippo.analytics.authorization.models.EntryPoint
import com.grippo.core.foundation.analytics.RetainedAnalytics
import com.grippo.screen.api.AuthRouter
import com.grippo.screen.api.AuthorizationEntryPoint
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.FlowId

/** Stateless UI adapter; each component context owns its retained scenario and saved state. */
internal class AuthAnalyticsProvider(private val analytics: AuthAnalytics) {

    fun provideAuthorization(
        context: ComponentContext,
        route: AuthRouter.AuthProcess,
    ): AuthAnalytics.Authorization =
        RetainedAnalytics(context).provide<AuthAnalytics.Authorization, AuthorizationState>(
            key = AUTH_STATE_KEY,
            serializer = AuthorizationState.serializer(),
            create = { restored ->
                analytics.authorization(
                    mapEntryPoint(route.entryPoint),
                    FlowId(route.flowId.value),
                    restored,
                )
            },
            validate = { scenario ->
                require(scenario.save().flow.id.value == route.flowId.value) {
                    "Retained authorization flow must match its route"
                }
            },
            save = { scenario ->
                scenario.save()
            },
        )

    fun provideSession(context: ComponentContext): AuthAnalytics.Session =
        RetainedAnalytics(context).provide<AuthAnalytics.Session, FlowContext>(
            key = SESSION_STATE_KEY,
            serializer = FlowContext.serializer(),
            create = analytics::session,
            save = { scenario ->
                scenario.save()
            },
        )

    private fun mapEntryPoint(entryPoint: AuthorizationEntryPoint): EntryPoint = when (entryPoint) {
        AuthorizationEntryPoint.LaunchWithoutSession -> EntryPoint.LaunchWithoutSession
        AuthorizationEntryPoint.TokenAbsent -> EntryPoint.TokenAbsent
        AuthorizationEntryPoint.SessionCheckFailed -> EntryPoint.SessionCheckFailed
        AuthorizationEntryPoint.ProfileSetupReturn -> EntryPoint.ProfileSetupReturn
        AuthorizationEntryPoint.RestoredScenario -> EntryPoint.RestoredScenario
    }

    private companion object {
        const val AUTH_STATE_KEY = "authorization_analytics_flow"
        const val SESSION_STATE_KEY = "authorization_session_flow"
    }
}
