package com.grippo.analytics.authorization

import com.grippo.analytics.authorization.internal.scenario.AuthAnalyticsImpl
import com.grippo.analytics.authorization.models.AuthTransition
import com.grippo.analytics.authorization.models.AuthorizationState
import com.grippo.analytics.authorization.models.EntryPoint
import com.grippo.analytics.authorization.models.ExitReason
import com.grippo.analytics.authorization.models.Step
import com.grippo.toolkit.analytics.context.*
import kotlin.test.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthNavigationTest {
    @Test fun reportingFailureCannotPreventFlowConstructionOrTransitions() {
        val recorder = RecordingAuthAnalytics()
        val observer = object : com.grippo.toolkit.analytics.Analytics by recorder {
            override fun track(
                event: com.grippo.toolkit.analytics.schema.AnalyticsEvent,
                flow: FlowContext?,
                operation: OperationId?
            ) { throw IllegalStateException("reporting defect") }
        }
        val scenario = AuthAnalyticsImpl(observer, TestClassifier)
            .authorization(EntryPoint.TokenAbsent, FlowId.create(), null)
        scenario.trackTransition(AuthTransition.OpenRegistration)
        assertEquals(Step.Registration, scenario.save().step)
        scenario.trackTransition(AuthTransition.BackToLogin)
        assertEquals(Step.Login, scenario.save().step)
        scenario.trackTransition(AuthTransition.Exit(ExitReason.Authenticated))
        assertTrue(scenario.save().exited)
    }

    @Test fun transitionsHaveActualReasonsAndImmutableAttribution() {
        val recorder = RecordingAuthAnalytics()
        val facade = AuthAnalyticsImpl(recorder, TestClassifier).authorization(EntryPoint.LaunchWithoutSession, FlowId.create(), null)
        val flow = facade.save().flow
        facade.presentScreen(Step.Login).close()
        facade.trackTransition(AuthTransition.OpenRegistration); facade.presentScreen(Step.Registration).close()
        facade.trackTransition(AuthTransition.BackToLogin); facade.presentScreen(Step.Login).close()
        facade.presentScreen(Step.Login).close()
        assertEquals(listOf(NavigationReason.Initial, NavigationReason.Action, NavigationReason.Back, NavigationReason.Restore), recorder.views.map { it.navigation.reason })
        assertEquals(ScreenId("auth_registration"), recorder.views[2].navigation.referrer)
        assertEquals(flow, facade.save().flow)
        assertEquals(ScreenId("auth_splash"), flow.originScreen)
    }
    @Test fun serializedRestoreDoesNotRestartFlowAndExitIsDeduplicated() {
        val first = authorization(RecordingAuthAnalytics())
        first.trackTransition(AuthTransition.OpenRegistration)
        val snapshot = Json.decodeFromString<AuthorizationState>(Json.encodeToString(first.save()))
        val recorder = RecordingAuthAnalytics()
        val restored = AuthAnalyticsImpl(recorder, TestClassifier).authorization(EntryPoint.TokenAbsent, snapshot.flow.id, snapshot)
        restored.presentScreen(Step.Registration).close()
        restored.trackTransition(AuthTransition.Exit(ExitReason.UserBack)); restored.trackTransition(AuthTransition.Exit(ExitReason.UserBack))
        assertEquals(NavigationReason.Restore, recorder.views.single().navigation.reason)
        assertEquals(listOf("auth_flow_exited"), recorder.names)
        assertEquals("registration", recorder.events.single().text("auth_step"))
        assertEquals(snapshot.flow, restored.save().flow)
    }
    @Test fun everyExitTransitionPreservesItsReasonAndHasOneTerminal() {
        for (reason in ExitReason.entries) {
            val recorder = RecordingAuthAnalytics()
            val analytics = authorization(recorder)
            analytics.trackTransition(AuthTransition.Exit(reason))
            analytics.trackTransition(AuthTransition.Exit(ExitReason.UserBack))
            val exits = recorder.events.filter { it.event.schema.name == "auth_flow_exited" }
            assertEquals(1, exits.size)
            assertEquals(reason.analyticsCode, exits.single().text("exit_reason"))
            assertEquals("login", exits.single().text("auth_step"))
            assertTrue(analytics.save().exited)
        }
    }

    @Test fun repeatedOrClosedTransitionsDoNotInventStepsOrOverwriteAttribution() {
        val recorder = RecordingAuthAnalytics()
        val analytics = authorization(recorder)
        val initial = analytics.save()
        analytics.trackTransition(AuthTransition.BackToLogin)
        assertEquals(initial, analytics.save())
        analytics.trackTransition(AuthTransition.OpenRegistration)
        analytics.presentScreen(Step.Registration).close()
        val registration = analytics.save()
        analytics.trackTransition(AuthTransition.OpenRegistration)
        assertEquals(registration, analytics.save())
        analytics.trackTransition(AuthTransition.BackToLogin)
        val login = analytics.save()
        analytics.trackTransition(AuthTransition.BackToLogin)
        assertEquals(login, analytics.save())
        assertEquals(listOf("auth_flow_started", "auth_step_changed", "auth_step_changed"), recorder.names)
        analytics.trackTransition(AuthTransition.Exit(ExitReason.UserBack))
        val closed = analytics.save()
        analytics.trackTransition(AuthTransition.OpenRegistration)
        analytics.trackTransition(AuthTransition.BackToLogin)
        assertEquals(closed, analytics.save())
        assertEquals("auth_flow_exited", recorder.names.last())
        assertEquals(4, recorder.events.size)
    }

    @Test fun allEntriesMapObservedOriginAndRestoreNeverInventsProvenance() {
        val expected = mapOf(
            EntryPoint.LaunchWithoutSession to "auth_splash",
            EntryPoint.SessionCheckFailed to "auth_splash",
            EntryPoint.ProfileSetupReturn to "profile_creation_user",
            EntryPoint.TokenAbsent to null,
            EntryPoint.RestoredScenario to null,
        )
        for (entry in EntryPoint.entries) {
            val recorder = RecordingAuthAnalytics()
            val facade = AuthAnalyticsImpl(recorder, TestClassifier).authorization(entry, FlowId.create(), null)
            facade.presentScreen(Step.Login).close()
            assertEquals(expected[entry], facade.save().flow.originScreen?.analyticsCode)
            if (entry == EntryPoint.ProfileSetupReturn) assertEquals(NavigationReason.Back, recorder.views.single().navigation.reason)
            if (entry == EntryPoint.RestoredScenario) assertEquals(NavigationReason.Restore, recorder.views.single().navigation.reason)
        }
    }
    @Test fun sessionRestoreKeepsFlowAndForegroundReason() {
        val first = session(RecordingAuthAnalytics())
        val recorder = RecordingAuthAnalytics()
        val restored = AuthAnalyticsImpl(recorder, TestClassifier).session(first.save())
        restored.presentScreen().close()
        assertEquals(first.save(), restored.save())
        assertEquals(NavigationReason.Restore, recorder.views.single().navigation.reason)
        assertTrue(recorder.events.isEmpty())
    }
    @Test fun restoreRejectsAnotherRoutesFlowBeforeReporting() {
        val snapshot = authorization(RecordingAuthAnalytics()).save()
        val recorder = RecordingAuthAnalytics()
        assertFailsWith<IllegalArgumentException> {
            AuthAnalyticsImpl(recorder, TestClassifier).authorization(
                EntryPoint.TokenAbsent, FlowId.create(), snapshot,
            )
        }
        assertTrue(recorder.events.isEmpty())
        assertTrue(recorder.views.isEmpty())
    }
}
