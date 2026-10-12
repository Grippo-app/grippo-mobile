package com.grippo.authorization.analytics

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.AuthAnalyticsModule
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.Method
import com.grippo.authorization.analytics.AuthorizationFixture.PreparationRequest
import com.grippo.core.error.provider.AppError
import com.grippo.core.foundation.CoreModule
import com.grippo.screen.api.AuthRouter
import com.grippo.screen.api.AuthorizationEntryPoint
import com.grippo.services.apple.auth.AppleAuthModule
import com.grippo.services.apple.auth.AppleAuthProvider
import com.grippo.toolkit.analytics.Analytics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.dsl.koinApplication
import org.koin.ksp.generated.module

internal fun factory(recorder: Analytics): AuthAnalytics {
    val app = koinApplication { modules(AuthAnalyticsModule().module) }
    try {
        app.koin.declare<Analytics>(recorder)
        return app.koin.get<AuthAnalytics>()
    } finally { app.close() }
}

class AuthIntegrationTest {
    @Test fun loginMethodsUseActualBusinessPreparationWithoutTelemetryContracts() = runTest {
        for (method in Method.entries) {
            val recorder = RecordingAuthAnalytics(); val analytics = factory(recorder)
            val domain = AuthorizationFixture(); val login = domain.loginUseCase()
            assertTrue(analytics.trackOperation(AuthOperation.Login(method)) {
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
                val result = factory(recorder).trackOperation(AuthOperation.Login(method)) {
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
            val operation: AuthOperation =
                if (register) AuthOperation.Register else AuthOperation.Login(Method.Email)
            assertSame(original, assertFailsWith<AppError.Network.Timeout> {
                factory(recorder).trackOperation(operation) {
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
                assertEquals(profile, factory(recorder).trackOperation(AuthOperation.Register) {
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
                val analytics = factory(recorder)
                val original = AppError.Network.Timeout("private")
                val domain = AuthorizationFixture().apply {
                    failAt(request, original)
                }
                val operation: AuthOperation =
                    if (register) AuthOperation.Register else AuthOperation.Login(Method.Email)
                assertSame(original, assertFailsWith<AppError.Network.Timeout> {
                    analytics.trackOperation(operation) {
                        if (register) domain.registerUseCase().createAccount("secret", "private")
                        else domain.loginUseCase().loginEmail("secret", "private")
                    }
                })
                assertEquals("auth_attempt_failed", recorder.names.last())
                assertTrue(recorder.events.last().event.parameters().none { it.key.name.startsWith("failure_") })
                assertFalse("auth_attempt_succeeded" in recorder.names)
                val method = if (register) "register" else "email"
                assertEquals(listOf(method) + requests.take(index + 1).map { it.call }, domain.calls)
            }
        }
    }
    @Test fun domainCancellationStopsLaterRequestsForEitherUseCase() = runTest {
        for (register in listOf(false, true)) {
            val recorder = RecordingAuthAnalytics()
            val analytics = factory(recorder)
            val domain = AuthorizationFixture().apply { beforeProfile = { awaitCancellation() } }
            val operation: AuthOperation =
                if (register) AuthOperation.Register else AuthOperation.Login(Method.Email)
            val job = launch {
                analytics.trackOperation(operation) {
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
    @Test
    fun navigationIdentityRejectsInvalidValuesIndependentlyOfAnalytics() {
        for (invalid in listOf("", "x".repeat(65), "has space", "has/slash")) {
            assertFailsWith<IllegalArgumentException> { com.grippo.screen.api.FlowId(invalid) }
        }
        for (valid in listOf("x", "x".repeat(64), "legacy-flow_1")) {
            val id = com.grippo.screen.api.FlowId(valid)
            assertEquals(id, Json.decodeFromString<com.grippo.screen.api.FlowId>(Json.encodeToString(id)))
        }
    }

    @Test
    fun routeIdentityKeepsLegacyWireShapeAndFreshGraphIdentity() {
        val route = AuthRouter.AuthProcess(flowId = com.grippo.screen.api.FlowId("legacy-flow"))
        val json = Json.encodeToString(route)
        assertTrue("\"flowId\":{\"value\":\"legacy-flow\"}" in json)
        assertEquals(route, Json.decodeFromString<AuthRouter.AuthProcess>(json))
        assertNotEquals(AuthRouter.AuthProcess().flowId, AuthRouter.AuthProcess().flowId)
    }

    @Test
    fun legacyRouteRemainsReadableWithoutTelemetryState() {
        val route = assertIs<AuthRouter.AuthProcess>(Json.decodeFromString<AuthRouter>(
            """{"type":"com.grippo.screen.api.AuthRouter.AuthProcess"}"""
        ))
        assertEquals(AuthorizationEntryPoint.RestoredScenario, route.entryPoint)
        assertEquals(route, Json.decodeFromString<AuthRouter>(Json.encodeToString<AuthRouter>(route)))
    }

    @Test
    fun appleProviderStillResolvesWithoutNetworkingOrContextDependencies() {
        val app = koinApplication { modules(AppleAuthModule().module) }
        try {
            assertNotNull(app.koin.get<AppleAuthProvider>())
        } finally {
            app.close()
        }
    }

    @Test
    fun generatedModulesResolveTheSharedStatelessService() {
        val recorder = RecordingAuthAnalytics()
        val app = koinApplication {
            modules(CoreModule().module, AuthAnalyticsModule().module)
        }
        try {
            app.koin.declare<Analytics>(recorder)
            assertSame(app.koin.get<AuthAnalytics>(), app.koin.get<AuthAnalytics>())
            assertTrue(recorder.events.isEmpty())
            assertTrue(recorder.views.isEmpty())
        } finally {
            app.close()
        }
    }
}
