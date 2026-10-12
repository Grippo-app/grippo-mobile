package com.grippo.authorization.analytics

import com.arkivanov.essenty.lifecycle.*
import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.AuthAnalyticsModule
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.AuthTransition
import com.grippo.analytics.authorization.models.AuthorizationState
import com.grippo.analytics.authorization.models.EntryPoint
import com.grippo.analytics.authorization.models.FailureCategory
import com.grippo.analytics.authorization.models.Method
import com.grippo.analytics.authorization.models.Step
import com.grippo.core.error.provider.AppError
import com.grippo.core.foundation.analytics.ScreenAnalytics
import com.grippo.core.foundation.CoreModule
import com.grippo.authorization.analytics.AuthorizationFixture.PreparationRequest
import com.grippo.screen.api.AuthRouter
import com.grippo.screen.api.AuthorizationEntryPoint
import com.grippo.services.apple.auth.*
import com.grippo.services.google.auth.GoogleAuthException
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.FlowId
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.dsl.koinApplication
import org.koin.ksp.generated.module

internal fun factory(recorder: Analytics): AuthAnalytics {
    val app = koinApplication { modules(AuthAnalyticsModule().module, AuthAnalyticsIntegrationModule().module) }
    try {
        app.koin.declare<Analytics>(recorder)
        return app.koin.get<AuthAnalytics>()
    } finally { app.close() }
}

class AuthIntegrationTest {
    private fun authorization(recorder: RecordingAuthAnalytics): AuthAnalytics.Authorization {
        val route = AuthRouter.AuthProcess(AuthorizationEntryPoint.TokenAbsent)
        return factory(recorder).authorization(EntryPoint.TokenAbsent, FlowId(route.flowId.value))
    }

    @Test fun loginMethodsUseActualBusinessPreparationWithoutTelemetryContracts() = runTest {
        for (method in Method.entries) {
            val recorder = RecordingAuthAnalytics(); val facade = authorization(recorder)
            val domain = AuthorizationFixture(); val login = domain.loginUseCase()
            assertTrue(facade.trackOperation(AuthOperation.Login(method)) {
                val credential = "secret"
                when (method) {
                    Method.Email -> login.loginEmail(credential, "private")
                    Method.Google -> login.loginGoogle(credential)
                    Method.Apple -> login.loginApple(credential)
                }
            })
            assertEquals(listOf(
                method.analyticsCode, "profile", "excluded_muscles", "excluded_equipments",
                "exercise_examples", "weight_history", "goal", "push_fetch", "push_register",
            ), domain.calls)
            assertEquals(listOf("auth_attempt_started", "auth_attempt_succeeded"), recorder.names.takeLast(2))
            assertFalse(recorder.events.flatMap { it.event.parameters() }.any { "secret" in it.value.toString() })
        }
    }
    @Test fun loginWithoutProfileReturnsFalseAfterPushAndSkipsData() = runTest {
        for (method in Method.entries) {
            for (token in listOf("secret_push", null)) {
                val recorder = RecordingAuthAnalytics()
                val domain = AuthorizationFixture().apply { hasProfile = false; pushToken = token }
                val login = domain.loginUseCase()
                val result = authorization(recorder).trackOperation(AuthOperation.Login(method)) {
                    when (method) {
                        Method.Email -> login.loginEmail("secret", "private")
                        Method.Google -> login.loginGoogle("secret")
                        Method.Apple -> login.loginApple("secret")
                    }
                }
                assertFalse(result)
                assertEquals(listOf(method.analyticsCode, "profile", "push_fetch") +
                    if (token == null) emptyList() else listOf("push_register"), domain.calls)
                assertEquals("auth_attempt_succeeded", recorder.names.last())
                assertFalse(recorder.names.any { it.startsWith("auth_setup_") })
            }
        }
    }
    @Test fun serverFailureDoesNotStartPreparationForEitherUseCase() = runTest {
        for (register in listOf(false, true)) {
            val recorder = RecordingAuthAnalytics()
            val original = AppError.Network.Timeout("private")
            val domain = AuthorizationFixture().apply { serverError = original }
            val operation: AuthOperation.Attempt =
                if (register) AuthOperation.Register else AuthOperation.Login(Method.Email)
            assertSame(original, assertFailsWith<AppError.Network.Timeout> {
                authorization(recorder).trackOperation(operation) {
                    if (register) domain.registerUseCase().createAccount("secret", "private")
                    else domain.loginUseCase().loginEmail("secret", "private")
                }
            })
            assertEquals(listOf(if (register) "register" else "email"), domain.calls)
            assertEquals("auth_attempt_failed", recorder.names.last())
        }
    }
    @Test fun registrationLoadsItsOwnDataAndMissingProfileSkipsDataButAttemptsPush() = runTest {
        for (profile in listOf(true, false)) {
            for (token in listOf("secret_push", null)) {
                val recorder = RecordingAuthAnalytics()
                val domain = AuthorizationFixture().apply { hasProfile = profile; pushToken = token }
                assertEquals(profile, authorization(recorder).trackOperation(AuthOperation.Register) {
                    domain.registerUseCase().createAccount("secret", "private")
                })
                val expected = mutableListOf("register", "profile")
                if (profile) expected += listOf("excluded_muscles", "excluded_equipments", "exercise_examples", "goal")
                expected += "push_fetch"
                if (token != null) expected += "push_register"
                assertEquals(expected, domain.calls)
                assertEquals("auth_attempt_succeeded", recorder.names.last())
                assertFalse(recorder.names.any { it.startsWith("auth_setup_") })
            }
        }
    }
    @Test fun preparationFailuresStopBothWholeAttemptsAndPreserveOriginal() = runTest {
        for (register in listOf(false, true)) {
            val requests = buildList {
                add(PreparationRequest.Profile)
                add(PreparationRequest.ExcludedMuscles)
                add(PreparationRequest.ExcludedEquipments)
                add(PreparationRequest.ExerciseExamples)
                if (!register) add(PreparationRequest.WeightHistory)
                add(PreparationRequest.Goal)
                add(PreparationRequest.PushFetch)
                add(PreparationRequest.PushRegister)
            }
            for ((index, request) in requests.withIndex()) {
                val recorder = RecordingAuthAnalytics()
                val facade = authorization(recorder)
                val original = AppError.Network.Timeout("private")
                val domain = AuthorizationFixture().apply {
                    failAt(request, original)
                }
                val operation: AuthOperation.Attempt =
                    if (register) AuthOperation.Register else AuthOperation.Login(Method.Email)
                assertSame(original, assertFailsWith<AppError.Network.Timeout> {
                    facade.trackOperation(operation) {
                        if (register) domain.registerUseCase().createAccount("secret", "private")
                        else domain.loginUseCase().loginEmail("secret", "private")
                    }
                })
                assertEquals("auth_attempt_failed", recorder.names.last())
                assertEquals("timeout", recorder.events.last().text("failure_category"))
                assertNull(recorder.events.last().text("failure_stage"))
                assertFalse("auth_attempt_succeeded" in recorder.names)
                val method = if (register) "register" else "email"
                assertEquals(listOf(method) + requests.take(index + 1).map { it.call }, domain.calls)
            }
        }
    }
    @Test fun domainCancellationStopsLaterRequestsForEitherUseCase() = runTest {
        for (register in listOf(false, true)) {
            val recorder = RecordingAuthAnalytics()
            val facade = authorization(recorder)
            val domain = AuthorizationFixture().apply { beforeProfile = { awaitCancellation() } }
            val operation: AuthOperation.Attempt =
                if (register) AuthOperation.Register else AuthOperation.Login(Method.Email)
            val job = launch {
                facade.trackOperation(operation) {
                    if (register) domain.registerUseCase().createAccount("secret", "private")
                    else domain.loginUseCase().loginEmail("secret", "private")
                }
            }
            runCurrent()
            job.cancelAndJoin()
            assertEquals(listOf(if (register) "register" else "email", "profile"), domain.calls)
            assertEquals("auth_attempt_interrupted", recorder.names.last())
        }
    }
    @Test fun sdkCancellationAndTypedNetworkErrorsAreMappedOnlyAtUiBoundary() {
        val classifier = AuthFailureClassifierImpl
        assertTrue(classifier.isProviderCancellation(GoogleAuthException.Cancelled("private")))
        assertTrue(classifier.isProviderCancellation(AppleAuthException("private", reason = AppleAuthFailureReason.Cancelled)))
        assertFalse(classifier.isProviderCancellation(IllegalStateException("cancelled")))
        assertEquals(FailureCategory.Offline, classifier.category(AppError.Network.NoInternet("private")))
        assertEquals(FailureCategory.Timeout, classifier.category(AppError.Network.Timeout("private")))
    }
    @Test fun snapshotRestoreKeepsIdentityWithoutReplayAndLeafLifecycleIsSingleBound() {
        val route = AuthRouter.AuthProcess(AuthorizationEntryPoint.TokenAbsent)
        val first = factory(RecordingAuthAnalytics()).authorization(EntryPoint.TokenAbsent, FlowId(route.flowId.value))
        first.trackTransition(AuthTransition.OpenRegistration)
        val recorder = RecordingAuthAnalytics()
        val snapshot = Json.decodeFromString<AuthorizationState>(Json.encodeToString(first.save()))
        val restored = factory(recorder).authorization(EntryPoint.TokenAbsent, FlowId(route.flowId.value), snapshot)
        val lifecycle = LifecycleRegistry()
        val app = koinApplication { modules(CoreModule().module) }
        try {
            app.koin.get<ScreenAnalytics>().bind(lifecycle) { restored.presentScreen(Step.Registration) }
        } finally {
            app.close()
        }
        lifecycle.resume(); lifecycle.pause(); lifecycle.resume(); lifecycle.destroy()
        assertEquals(2, recorder.views.size)
        assertTrue(recorder.views.all { it.flow?.id?.value == route.flowId.value })
        assertEquals(listOf(NavigationReason.Restore, NavigationReason.Restore), recorder.views.map { it.navigation.reason })
        assertTrue(recorder.events.isEmpty())
    }
    @Test fun sessionSnapshotRestoreKeepsFlowAndExplicitRestoreReason() {
        val first = factory(RecordingAuthAnalytics()).session()
        val recorder = RecordingAuthAnalytics()
        val restored = factory(recorder).session(first.save())
        restored.presentScreen().close()
        assertEquals(first.save(), restored.save())
        assertEquals(NavigationReason.Restore, recorder.views.single().navigation.reason)
    }
    @Test fun navigationIdentityRejectsInvalidValuesBeforeAnalyticsMapping() {
        for (invalid in listOf("", "x".repeat(65), "has space", "has/slash")) {
            assertFailsWith<IllegalArgumentException> { com.grippo.screen.api.FlowId(invalid) }
        }
        for (valid in listOf("x", "x".repeat(64), "legacy-flow_1")) {
            val id = com.grippo.screen.api.FlowId(valid)
            assertEquals(id.value, FlowId(id.value).value)
        }
    }

    @Test fun routeIdentityKeepsLegacyWireShapeWithoutAnalyticsType() {
        val route = AuthRouter.AuthProcess(
            flowId = com.grippo.screen.api.FlowId("legacy-flow")
        )
        val json = Json.encodeToString(route)
        assertTrue("\"flowId\":{\"value\":\"legacy-flow\"}" in json)
        assertEquals(route, Json.decodeFromString<AuthRouter.AuthProcess>(json))
        val scenario = AuthAnalyticsProvider(factory(RecordingAuthAnalytics())).provideAuthorization(
            com.arkivanov.decompose.DefaultComponentContext(LifecycleRegistry()), route
        )
        assertEquals(route.flowId.value, scenario.save().flow.id.value)
    }

    @Test fun legacyRouteAndFlowSnapshotRemainReadable() {
        val route = assertIs<AuthRouter.AuthProcess>(Json.decodeFromString<AuthRouter>("""{"type":"com.grippo.screen.api.AuthRouter.AuthProcess"}"""))
        assertEquals(AuthorizationEntryPoint.RestoredScenario, route.entryPoint)
        val facade = factory(RecordingAuthAnalytics()).authorization(EntryPoint.RestoredScenario, FlowId(route.flowId.value))
        assertEquals("restored_scenario", facade.save().flow.entryPoint.analyticsCode)
        assertNull(facade.save().flow.originScreen)
        val json = Json.encodeToString(facade.save())
        assertEquals(facade.save(), Json.decodeFromString<AuthorizationState>(json))
        assertNotEquals(route.flowId, AuthRouter.AuthProcess().flowId)
        val legacySnapshot = Json.decodeFromString<AuthorizationState>("""{
          "flow":{"id":{"value":"legacy-flow"},"entryPoint":{"analyticsCode":"token_absent"},"originScreen":null},
          "login":{"screen":{"analyticsCode":"auth_login"},"reason":"Initial"},
          "registration":{"screen":{"analyticsCode":"auth_registration"},"referrer":{"analyticsCode":"auth_login"},"reason":"Action"},
          "step":"Registration","exited":false
        }""")
        assertEquals("legacy-flow", legacySnapshot.flow.id.value)
        assertEquals(Step.Registration, legacySnapshot.step)
        assertEquals("com.grippo.authorization.analytics.internal.AuthFlowState", AuthorizationState.serializer().descriptor.serialName)
        val recorder = RecordingAuthAnalytics()
        val restored = factory(recorder).authorization(EntryPoint.TokenAbsent, legacySnapshot.flow.id, legacySnapshot)
        restored.presentScreen(Step.Registration).close()
        assertTrue(recorder.events.isEmpty())
        assertEquals(NavigationReason.Restore, recorder.views.single().navigation.reason)

    }
    @Test fun appleProviderStillResolvesWithoutNetworkingOrContextDependencies() {
        val app = koinApplication { modules(AppleAuthModule().module) }
        try { assertNotNull(app.koin.get<AppleAuthProvider>()) } finally { app.close() }
    }
    @Test fun generatedModulesResolveSingletonFactoryAndSeparateScenarioInstances() {
        val recorder = RecordingAuthAnalytics()
        val app = koinApplication { modules(AuthAnalyticsModule().module, AuthAnalyticsIntegrationModule().module) }
        try {
            app.koin.declare<Analytics>(recorder)
            val first = app.koin.get<AuthAnalytics>()
            assertSame(first, app.koin.get<AuthAnalytics>())
            assertSame(app.koin.get<AuthAnalyticsProvider>(), app.koin.get<AuthAnalyticsProvider>())
            val left = first.session(); val right = first.session()
            assertNotSame(left, right)
            assertNotEquals(left.save().id, right.save().id)
        } finally { app.close() }
    }
}
