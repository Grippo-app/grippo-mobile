package com.grippo.authorization.analytics

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.grippo.analytics.authorization.models.AuthTransition
import com.grippo.analytics.authorization.models.ExitReason
import com.grippo.analytics.authorization.models.Step
import com.grippo.screen.api.AuthRouter
import com.grippo.screen.api.AuthorizationEntryPoint
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class AuthAnalyticsProviderTest {
    @Test
    fun recreationRetainsAuthorizationAndRegistersItsLatestState() {
        val recorder = RecordingAuthAnalytics()
        val provider = AuthAnalyticsProvider(factory(recorder))
        val route = AuthRouter.AuthProcess(AuthorizationEntryPoint.TokenAbsent)
        val instances = InstanceKeeperDispatcher()
        val firstState = StateKeeperDispatcher()
        val first = provider.provideAuthorization(DefaultComponentContext(LifecycleRegistry(), firstState, instances), route)
        val nextState = StateKeeperDispatcher(firstState.save())
        val next = provider.provideAuthorization(DefaultComponentContext(LifecycleRegistry(), nextState, instances), route)
        assertSame(first, next)
        first.trackTransition(AuthTransition.OpenRegistration)
        first.trackTransition(AuthTransition.Exit(ExitReason.ProfileRequired))
        val restored = provider.provideAuthorization(
            DefaultComponentContext(LifecycleRegistry(), StateKeeperDispatcher(nextState.save())), route,
        )
        assertEquals(first.save().copy(
            login = first.save().login.copy(reason = NavigationReason.Restore),
            registration = first.save().registration.copy(reason = NavigationReason.Restore),
        ), restored.save())
        assertEquals(Step.Registration, restored.save().step)
        assertEquals(1, recorder.names.count { it == "auth_flow_started" })
        assertEquals(1, recorder.names.count { it == "auth_flow_exited" })
    }

    @Test
    fun retainedAuthorizationCannotBeReboundToAnotherRoute() {
        val recorder = RecordingAuthAnalytics()
        val provider = AuthAnalyticsProvider(factory(recorder))
        val instances = InstanceKeeperDispatcher()
        provider.provideAuthorization(
            DefaultComponentContext(LifecycleRegistry(), instanceKeeper = instances),
            AuthRouter.AuthProcess(AuthorizationEntryPoint.TokenAbsent),
        )
        assertFailsWith<IllegalArgumentException> {
            provider.provideAuthorization(
                DefaultComponentContext(LifecycleRegistry(), instanceKeeper = instances),
                AuthRouter.AuthProcess(AuthorizationEntryPoint.TokenAbsent),
            )
        }
        assertEquals(listOf("auth_flow_started"), recorder.names)
    }

    @Test
    fun sessionRetainsAcrossRecreationRestoresSnapshotAndIsolatesOwners() {
        val provider = AuthAnalyticsProvider(factory(RecordingAuthAnalytics()))
        val state = StateKeeperDispatcher()
        val instances = InstanceKeeperDispatcher()
        val first = provider.provideSession(DefaultComponentContext(LifecycleRegistry(), state, instances))
        val nextState = StateKeeperDispatcher(state.save())
        val next = provider.provideSession(DefaultComponentContext(LifecycleRegistry(), nextState, instances))
        assertSame(first, next)
        val restored = provider.provideSession(DefaultComponentContext(LifecycleRegistry(), StateKeeperDispatcher(nextState.save())))
        assertEquals(first.save(), restored.save())
        val independent = provider.provideSession(DefaultComponentContext(LifecycleRegistry()))
        assertNotEquals(first.save().id, independent.save().id)
    }

    @Test
    fun routeEntryPointsAreMappedWithoutGuessing() {
        val provider = AuthAnalyticsProvider(factory(RecordingAuthAnalytics()))
        for ((routeEntry, expected) in listOf(
            AuthorizationEntryPoint.LaunchWithoutSession to "launch_without_session",
            AuthorizationEntryPoint.TokenAbsent to "token_absent",
            AuthorizationEntryPoint.SessionCheckFailed to "session_check_failed",
            AuthorizationEntryPoint.ProfileSetupReturn to "profile_setup_return",
            AuthorizationEntryPoint.RestoredScenario to "restored_scenario",
        )) {
            val analytics = provider.provideAuthorization(
                DefaultComponentContext(LifecycleRegistry()), AuthRouter.AuthProcess(routeEntry),
            )
            assertEquals(expected, analytics.save().flow.entryPoint.analyticsCode)
        }
    }
}
