package com.grippo.authorization.analytics

import com.grippo.authorization.splash.SplashDirection
import com.grippo.authorization.splash.SplashLoader
import com.grippo.authorization.splash.SplashViewModel
import com.grippo.core.error.provider.AppError
import com.grippo.core.error.provider.ErrorProvider
import com.grippo.core.foundation.CoreModule
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.user.models.RoleEnum
import com.grippo.data.features.api.user.models.User
import com.grippo.data.features.api.user.models.UserStats
import com.grippo.screen.api.AuthorizationEntryPoint
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.OperationId
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.crashreporting.CrashReporting
import com.grippo.toolkit.crashreporting.context.CrashReportContext
import com.grippo.toolkit.crashreporting.values.CrashDiagnosticCode
import kotlin.test.*
import kotlin.time.Duration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDateTime
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module

class SplashViewModelTest {
    private class DataFixture : MuscleFeature, EquipmentFeature {
        var muscleFailure: Exception? = null
        var equipmentFailure: Exception? = null
        var suspended = false
        var equipmentCancelled = false
        val requests = mutableListOf<String>()
        val calls: Int get() = requests.size
        override suspend fun getMuscles(): Result<Unit> {
            requests += "muscles"
            yield()
            return muscleFailure?.let { Result.failure(it) } ?: Result.success(Unit)
        }
        override suspend fun getEquipments(): Result<Unit> {
            requests += "equipments"
            if (suspended) try { awaitCancellation() } finally { equipmentCancelled = true }
            return equipmentFailure?.let { Result.failure(it) } ?: Result.success(Unit)
        }
        override fun observeMuscles(): Flow<List<MuscleGroup>> = error("Unused fixture call")
        override fun observeEquipments(): Flow<List<EquipmentGroup>> = error("Unused fixture call")
    }

    private fun TestScope.withSplash(
        domain: AuthorizationFixture,
        data: DataFixture = DataFixture(),
        rejectReporting: Boolean = false,
        verify: TestScope.(SplashViewModel, RecordingAuthAnalytics, List<Throwable>, List<SplashDirection>, List<() -> Unit>) -> Unit,
    ) {
        val errors = mutableListOf<Throwable>()
        val crashes = mutableListOf<Throwable>()
        val directions = mutableListOf<SplashDirection>()
        val dismissCallbacks = mutableListOf<() -> Unit>()
        val recorder = RecordingAuthAnalytics()
        val reporting = if (rejectReporting) {
            object : Analytics by recorder {
                override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?) {
                    throw IllegalStateException("Reporting failed")
                }
            }
        } else {
            recorder
        }
        val analytics = factory(reporting)
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        startKoin { modules(CoreModule().module, module {
            single<ErrorProvider> { object : ErrorProvider {
                override suspend fun provide(exception: Throwable, callback: () -> Unit) {
                    errors += exception
                    dismissCallbacks += callback
                }
            } }
            single<CrashReporting> { object : CrashReporting {
                override fun reportException(throwable: Throwable, context: CrashReportContext) {
                    crashes += throwable
                }
                override fun logBreadcrumb(code: CrashDiagnosticCode) = Unit
            } }
        }) }
        var viewModel: SplashViewModel? = null
        try {
            val splash = SplashViewModel(analytics, data, data, domain, domain)
            viewModel = splash
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                splash.navigator.collect { directions += it }
            }
            verify(splash, recorder, errors, directions, dismissCallbacks)
            assertEquals(errors, crashes, "Dialogs and crash reporting must receive the same original failures")
        } finally {
            viewModel?.onDestroy()
            stopKoin()
            Dispatchers.resetMain()
        }
    }

    @Test fun businessDecisionsDriveNavigationAndOnlyMissingSessionLoadsData() = runTest {
        for (profile in listOf(true, false)) {
            val data = DataFixture()
            withSplash(AuthorizationFixture().apply { hasProfile = profile }, data) { _, analytics, errors, directions, _ ->
                advanceUntilIdle()
                assertEquals(listOf(if (profile) SplashDirection.Home else SplashDirection.CreateProfile), directions)
                assertEquals(0, data.calls)
                assertTrue(errors.isEmpty())
                assertEquals(listOf("auth_session_check_started", "auth_session_check_completed"), analytics.names)
            }
        }
        val domain = AuthorizationFixture().apply { tokenFlow = flowOf(null) }
        val data = DataFixture()
        withSplash(domain, data) { _, analytics, errors, directions, _ ->
            advanceUntilIdle()
            assertEquals(listOf(SplashDirection.AuthProcess(AuthorizationEntryPoint.LaunchWithoutSession)), directions)
            assertTrue(domain.calls.isEmpty())
            assertEquals(listOf("muscles", "equipments"), data.requests)
            assertTrue(errors.isEmpty())
            assertTrue(analytics.events.isEmpty())
        }
    }

    @Test fun cachedProfileSkipsRemoteProfileAndInitialData() = runTest {
        val user = User(
            id = "fixture_user", name = "Fixture", email = "fixture@example.com",
            experience = ExperienceEnum.BEGINNER, weight = 70f, height = 170,
            createdAt = LocalDateTime(2026, 1, 1, 0, 0), role = RoleEnum.DEFAULT,
            stats = UserStats(0, Duration.ZERO, 0f, 0),
        )
        val domain = AuthorizationFixture().apply {
            userFlow = flowOf(user)
            profileError = IllegalStateException("Remote profile must not be requested")
        }
        val data = DataFixture()
        withSplash(domain, data) { _, analytics, errors, directions, _ ->
            advanceUntilIdle()
            assertEquals(listOf(SplashDirection.Home), directions)
            assertTrue(domain.calls.isEmpty())
            assertEquals(0, data.calls)
            assertTrue(errors.isEmpty())
            assertEquals(listOf("auth_session_check_started", "auth_session_check_completed"), analytics.names)
        }
    }

    @Test
    fun profileLookupFailuresUseTheStandardDialogAndRetryAfterDismissal() = runTest {
        val original = AppError.Network.Timeout("private")
        val domains = listOf(
            AuthorizationFixture().apply {
                profileError = original
            },
            AuthorizationFixture().apply {
                userFlow = flow {
                    throw original
                }
            },
        )

        for (domain in domains) {
            val data = DataFixture()
            var tokenRequests = 0
            domain.tokenFlow = flow {
                tokenRequests++
                emit("fixture_session")
            }
            withSplash(domain, data) { splash, analytics, errors, directions, callbacks ->
                advanceUntilIdle()

                val reported = errors.single()
                assertSame(original, if (reported.cause === original) reported.cause else reported)
                assertTrue(directions.isEmpty())
                assertEquals(0, data.calls)
                assertTrue(splash.loaders.value.isEmpty())
                assertEquals(1, callbacks.size)
                assertEquals(1, tokenRequests)
                assertEquals(
                    listOf("auth_session_check_started", "auth_session_check_failed"),
                    analytics.names,
                )
                assertTrue(analytics.events.last().event.parameters().none { it.key.name.startsWith("failure_") })
                assertNull(analytics.events.last().text("session_outcome"))

                val profileRequestsBeforeRetry = domain.calls.size
                domain.profileError = null
                domain.userFlow = flowOf(null)
                callbacks.single().invoke()
                advanceUntilIdle()

                assertEquals(2, tokenRequests)
                assertEquals(profileRequestsBeforeRetry + 1, domain.calls.size)
                assertEquals(listOf(SplashDirection.Home), directions)
                assertEquals(0, data.calls)
                assertEquals(1, errors.size)
                assertTrue(splash.loaders.value.isEmpty())
                assertEquals(
                    listOf(
                        "auth_session_check_started", "auth_session_check_failed",
                        "auth_session_check_started", "auth_session_check_completed",
                    ),
                    analytics.names,
                )
                val firstAttempt = assertNotNull(analytics.events[0].operation)
                val retryAttempt = assertNotNull(analytics.events[2].operation)
                assertEquals(firstAttempt, analytics.events[1].operation)
                assertEquals(retryAttempt, analytics.events[3].operation)
                assertNotEquals(firstAttempt, retryAttempt)
                assertTrue(analytics.events.all { it.flow == null })
            }
        }
    }

    @Test
    fun profileRetryRechecksTheTokenBeforeLoadingAnyProfile() = runTest {
        val domain = AuthorizationFixture().apply {
            profileError = AppError.Network.Timeout("Profile failed")
        }
        val data = DataFixture()
        withSplash(domain, data) { splash, analytics, errors, directions, callbacks ->
            advanceUntilIdle()
            assertEquals(1, errors.size)
            assertTrue(directions.isEmpty())
            assertEquals(listOf("profile"), domain.calls)
            assertTrue(data.requests.isEmpty())

            domain.tokenFlow = flowOf(null)
            callbacks.single().invoke()
            advanceUntilIdle()

            assertEquals(listOf("profile"), domain.calls)
            assertEquals(listOf("muscles", "equipments"), data.requests)
            assertEquals(
                listOf(SplashDirection.AuthProcess(AuthorizationEntryPoint.LaunchWithoutSession)),
                directions,
            )
            assertEquals(1, errors.size)
            assertTrue(splash.loaders.value.isEmpty())
            assertEquals(listOf("auth_session_check_started", "auth_session_check_failed"), analytics.names)
        }
    }

    @Test
    fun profileDialogCannotRestartADestroyedViewModel() = runTest {
        val domain = AuthorizationFixture().apply {
            profileError = AppError.Network.Timeout("Profile failed")
        }
        withSplash(domain) { splash, analytics, errors, directions, callbacks ->
            advanceUntilIdle()
            assertEquals(1, errors.size)
            assertEquals(listOf("profile"), domain.calls)
            assertTrue(directions.isEmpty())

            splash.onDestroy()
            callbacks.single().invoke()
            advanceUntilIdle()

            assertEquals(listOf("profile"), domain.calls)
            assertEquals(1, errors.size)
            assertTrue(directions.isEmpty())
            assertTrue(splash.loaders.value.isEmpty())
            assertEquals(listOf("auth_session_check_started", "auth_session_check_failed"), analytics.names)
        }
    }

    @Test
    fun tokenFailureRetriesOnlyAfterErrorDismissal() = runTest {
        val original = IllegalStateException("private")
        val domain = AuthorizationFixture()
        val validTokenFlow = domain.tokenFlow
        domain.tokenFlow = flow {
            throw original
        }
        withSplash(domain) { _, analytics, errors, directions, callbacks ->
            advanceUntilIdle()
            // JVM coroutine stack recovery may copy an exception and retain the original as its cause.
            val reported = errors.single()
            assertSame(original, if (reported.cause === original) reported.cause else reported)
            assertTrue(directions.isEmpty())
            assertTrue(domain.calls.isEmpty())
            assertTrue(analytics.events.isEmpty())
            domain.tokenFlow = validTokenFlow
            callbacks.single().invoke()
            advanceUntilIdle()
            assertEquals(listOf(SplashDirection.Home), directions)
            assertEquals(listOf("auth_session_check_started", "auth_session_check_completed"), analytics.names)
        }
    }

    @Test
    fun fatalProfileErrorsUseTheStandardPipeline() = runTest {
        val original = AssertionError("Fatal profile failure")
        val domain = AuthorizationFixture().apply {
            beforeProfile = {
                throw original
            }
        }
        val data = DataFixture()
        withSplash(domain, data) { splash, analytics, errors, directions, callbacks ->
            advanceUntilIdle()

            val reported = errors.single()
            assertSame(original, if (reported.cause === original) reported.cause else reported)
            assertEquals(1, callbacks.size)
            assertTrue(directions.isEmpty())
            assertEquals(0, data.calls)
            assertTrue(splash.loaders.value.isEmpty())
            assertEquals(listOf("auth_session_check_started"), analytics.names)
        }
    }

    @Test
    fun reportingFailuresCannotChangeProfileSuccessOrErrorHandling() = runTest {
        for (profileFails in listOf(false, true)) {
            val original = AppError.Network.Timeout("Profile failed")
            val domain = AuthorizationFixture().apply {
                if (profileFails) {
                    profileError = original
                }
            }
            withSplash(domain, rejectReporting = true) { splash, analytics, errors, directions, callbacks ->
                advanceUntilIdle()

                if (profileFails) {
                    val reported = errors.single()
                    assertSame(original, if (reported.cause === original) reported.cause else reported)
                    assertEquals(1, callbacks.size)
                    assertTrue(directions.isEmpty())

                    domain.profileError = null
                    callbacks.single().invoke()
                    advanceUntilIdle()

                    assertEquals(listOf("profile", "profile"), domain.calls)
                    assertEquals(1, errors.size)
                } else {
                    assertEquals(listOf("profile"), domain.calls)
                    assertTrue(errors.isEmpty())
                    assertTrue(callbacks.isEmpty())
                }
                assertEquals(listOf(SplashDirection.Home), directions)
                assertTrue(splash.loaders.value.isEmpty())
                assertTrue(analytics.events.isEmpty())
            }
        }
    }

    @Test
    fun profileCancellationDoesNotOpenADialogOrNavigate() = runTest {
        val domain = AuthorizationFixture().apply {
            profileError = CancellationException("private")
        }
        withSplash(domain) { _, analytics, errors, directions, callbacks ->
            advanceUntilIdle()
            assertTrue(errors.isEmpty())
            assertTrue(directions.isEmpty())
            assertTrue(callbacks.isEmpty())
            assertEquals("auth_session_check_interrupted", analytics.names.last())
        }
    }

    @Test
    fun destructionInterruptsAPendingProfileLookup() = runTest {
        var cancelled = false
        val domain = AuthorizationFixture().apply {
            beforeProfile = {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            }
        }
        val data = DataFixture()
        withSplash(domain, data) { splash, analytics, errors, directions, callbacks ->
            runCurrent()
            assertEquals(listOf("profile"), domain.calls)
            assertEquals(setOf<SplashLoader>(SplashLoader.AppContent), splash.loaders.value)
            assertEquals(listOf("auth_session_check_started"), analytics.names)

            splash.onDestroy()
            runCurrent()

            assertTrue(cancelled)
            assertTrue(splash.loaders.value.isEmpty())
            assertTrue(errors.isEmpty())
            assertTrue(callbacks.isEmpty())
            assertTrue(directions.isEmpty())
            assertEquals(0, data.calls)
            assertEquals("auth_session_check_interrupted", analytics.names.last())
        }
    }

    @Test fun firstRequestFailurePreservesErrorAndSkipsSecondRequest() = runTest {
        val original = AppError.Network.Timeout("private")
        val data = DataFixture().apply { muscleFailure = original; suspended = true }
        withSplash(AuthorizationFixture().apply { tokenFlow = flowOf(null) }, data) { _, analytics, errors, directions, callbacks ->
            advanceUntilIdle()
            // JVM coroutine stack recovery may copy an exception and retain the original as its cause.
            val reported = errors.single()
            assertSame(original, if (reported.cause === original) reported.cause else reported)
            assertEquals(listOf("muscles"), data.requests)
            assertFalse(data.equipmentCancelled)
            assertTrue(directions.isEmpty())
            assertTrue(analytics.events.isEmpty())
            data.muscleFailure = null
            data.suspended = false
            callbacks.single().invoke()
            advanceUntilIdle()
            assertEquals(listOf("muscles", "muscles", "equipments"), data.requests)
            assertEquals(listOf(SplashDirection.AuthProcess(AuthorizationEntryPoint.LaunchWithoutSession)), directions)
            assertTrue(analytics.events.isEmpty())
        }
    }

    @Test fun viewModelDestructionInterruptsPendingDataLoad() = runTest {
        val data = DataFixture().apply { suspended = true }
        withSplash(AuthorizationFixture().apply { tokenFlow = flowOf(null) }, data) { splash, analytics, errors, directions, _ ->
            runCurrent()
            splash.onDestroy()
            runCurrent()
            assertTrue(data.equipmentCancelled)
            assertTrue(errors.isEmpty())
            assertTrue(directions.isEmpty())
            assertTrue(analytics.events.isEmpty())
        }
    }

    @Test fun equipmentFailureCannotBypassRequiredDataAndRetriesTheWholeSequence() = runTest {
        val original = AppError.Network.NoInternet("private")
        val data = DataFixture().apply { equipmentFailure = original }
        withSplash(AuthorizationFixture().apply { tokenFlow = flowOf(null) }, data) { _, analytics, errors, directions, callbacks ->
            advanceUntilIdle()
            val reported = errors.single()
            assertSame(original, if (reported.cause === original) reported.cause else reported)
            assertEquals(listOf("muscles", "equipments"), data.requests)
            assertTrue(directions.isEmpty())
            assertTrue(analytics.events.isEmpty())

            data.equipmentFailure = null
            callbacks.single().invoke()
            advanceUntilIdle()

            assertEquals(listOf("muscles", "equipments", "muscles", "equipments"), data.requests)
            assertEquals(listOf(SplashDirection.AuthProcess(AuthorizationEntryPoint.LaunchWithoutSession)), directions)
            assertTrue(analytics.events.isEmpty())
        }
    }

    @Test fun retryWaitsForDismissalAndStaleDialogCannotRestartDestroyedViewModel() = runTest {
        var tokenRequests = 0
        val domain = AuthorizationFixture().apply {
            tokenFlow = flow {
                tokenRequests++
                throw AppError.Network.Timeout("private")
            }
        }
        withSplash(domain) { splash, analytics, errors, directions, callbacks ->
            advanceUntilIdle()
            assertEquals(1, tokenRequests)
            assertTrue(domain.calls.isEmpty())
            assertEquals(1, errors.size)
            assertTrue(directions.isEmpty())

            callbacks.single().invoke()
            advanceUntilIdle()
            assertEquals(2, tokenRequests)
            assertTrue(domain.calls.isEmpty())
            assertEquals(2, errors.size)
            assertEquals(2, callbacks.size)
            assertTrue(directions.isEmpty())
            assertTrue(splash.loaders.value.isEmpty())
            assertTrue(analytics.events.isEmpty())

            splash.onDestroy()
            callbacks.last().invoke()
            advanceUntilIdle()
            assertEquals(2, tokenRequests)
            assertTrue(domain.calls.isEmpty())
            assertEquals(2, errors.size)
            assertTrue(directions.isEmpty())
            assertTrue(splash.loaders.value.isEmpty())
        }
    }
}
