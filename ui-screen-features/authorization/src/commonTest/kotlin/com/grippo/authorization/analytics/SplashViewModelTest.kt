package com.grippo.authorization.analytics

import com.grippo.authorization.splash.SplashDirection
import com.grippo.authorization.splash.SplashViewModel
import com.grippo.core.error.provider.AppError
import com.grippo.core.error.provider.ErrorProvider
import com.grippo.core.foundation.CoreModule
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.screen.api.AuthorizationEntryPoint
import com.grippo.toolkit.crashreporting.CrashReporting
import com.grippo.toolkit.crashreporting.context.CrashReportContext
import com.grippo.toolkit.crashreporting.values.CrashDiagnosticCode
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module

class SplashViewModelTest {
    private class DataFixture : MuscleFeature, EquipmentFeature {
        var failure: Exception? = null
        var suspended = false
        var equipmentCancelled = false
        val requests = mutableListOf<String>()
        val calls: Int get() = requests.size
        override suspend fun getMuscles(): Result<Unit> {
            requests += "muscles"
            yield()
            return failure?.let { Result.failure(it) } ?: Result.success(Unit)
        }
        override suspend fun getEquipments(): Result<Unit> {
            requests += "equipments"
            if (suspended) try { awaitCancellation() } finally { equipmentCancelled = true }
            return Result.success(Unit)
        }
        override fun observeMuscles(): Flow<List<MuscleGroup>> = error("Unused fixture call")
        override fun observeEquipments(): Flow<List<EquipmentGroup>> = error("Unused fixture call")
    }

    private fun TestScope.withSplash(
        domain: AuthorizationFixture,
        data: DataFixture = DataFixture(),
        verify: TestScope.(SplashViewModel, RecordingAuthAnalytics, List<Throwable>, List<SplashDirection>) -> Unit,
    ) {
        val errors = mutableListOf<Throwable>()
        val directions = mutableListOf<SplashDirection>()
        val analytics = RecordingAuthAnalytics()
        val scenario = factory(analytics).session()
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        startKoin { modules(CoreModule().module, module {
            single<ErrorProvider> { object : ErrorProvider {
                override suspend fun provide(exception: Throwable, callback: () -> Unit) { errors += exception }
            } }
            single<CrashReporting> { object : CrashReporting {
                override fun reportException(throwable: Throwable, context: CrashReportContext) = Unit
                override fun logBreadcrumb(code: CrashDiagnosticCode) = Unit
            } }
        }) }
        var viewModel: SplashViewModel? = null
        try {
            val splash = SplashViewModel(scenario, data, data, domain, domain)
            viewModel = splash
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                splash.navigator.collect { directions += it }
            }
            verify(splash, analytics, errors, directions)
        } finally {
            viewModel?.onDestroy()
            stopKoin()
            Dispatchers.resetMain()
        }
    }

    @Test fun businessDecisionsDriveNavigationAndOnlyMissingSessionLoadsData() = runTest {
        for (profile in listOf(true, false)) {
            val data = DataFixture()
            withSplash(AuthorizationFixture().apply { hasProfile = profile }, data) { _, analytics, errors, directions ->
                advanceUntilIdle()
                assertEquals(listOf(if (profile) SplashDirection.Home else SplashDirection.CreateProfile), directions)
                assertEquals(0, data.calls)
                assertTrue(errors.isEmpty())
                assertEquals(listOf("auth_session_check_started", "auth_session_check_completed"), analytics.names)
            }
        }
        val domain = AuthorizationFixture().apply { tokenFlow = flowOf(null) }
        val data = DataFixture()
        withSplash(domain, data) { _, analytics, errors, directions ->
            advanceUntilIdle()
            assertEquals(listOf(SplashDirection.AuthProcess(AuthorizationEntryPoint.LaunchWithoutSession)), directions)
            assertTrue(domain.calls.isEmpty())
            assertEquals(listOf("muscles", "equipments"), data.requests)
            assertTrue(errors.isEmpty())
            assertEquals(listOf("auth_session_check_started", "auth_session_check_completed"), analytics.names)
        }
    }

    @Test fun profileFailureIsNotMissingSessionAndTokenFailureKeepsOriginalError() = runTest {
        withSplash(AuthorizationFixture().apply { profileError = AppError.Network.Timeout("private") }) { _, analytics, errors, directions ->
            advanceUntilIdle()
            assertEquals(listOf(SplashDirection.AuthProcess(AuthorizationEntryPoint.SessionCheckFailed)), directions)
            assertTrue(errors.isEmpty())
            assertEquals("auth_session_check_completed", analytics.names.last())
            assertNull(analytics.events.last().text("session_outcome"))
        }
        val original = IllegalStateException("private")
        val domain = AuthorizationFixture().apply { tokenFlow = flow { throw original } }
        withSplash(domain) { _, analytics, errors, directions ->
            advanceUntilIdle()
            // JVM coroutine stack recovery may copy an exception and retain the original as its cause.
            val reported = errors.single()
            assertSame(original, if (reported.cause === original) reported.cause else reported)
            assertTrue(directions.isEmpty())
            assertTrue(domain.calls.isEmpty())
            assertEquals("auth_session_check_failed", analytics.names.last())
            assertNull(analytics.events.last().text("failure_stage"))
        }
        withSplash(AuthorizationFixture().apply { profileError = CancellationException("private") }) { _, analytics, errors, directions ->
            advanceUntilIdle()
            assertTrue(errors.isEmpty())
            assertTrue(directions.isEmpty())
            assertEquals("auth_session_check_interrupted", analytics.names.last())
        }
    }

    @Test fun firstRequestFailurePreservesErrorAndSkipsSecondRequest() = runTest {
        val original = AppError.Network.Timeout("private")
        val data = DataFixture().apply { failure = original; suspended = true }
        withSplash(AuthorizationFixture().apply { tokenFlow = flowOf(null) }, data) { _, analytics, errors, directions ->
            advanceUntilIdle()
            // JVM coroutine stack recovery may copy an exception and retain the original as its cause.
            val reported = errors.single()
            assertSame(original, if (reported.cause === original) reported.cause else reported)
            assertEquals(listOf("muscles"), data.requests)
            assertFalse(data.equipmentCancelled)
            assertTrue(directions.isEmpty())
            assertEquals(listOf("auth_session_check_started", "auth_session_check_completed"), analytics.names)
        }
    }

    @Test fun viewModelDestructionInterruptsPendingDataLoad() = runTest {
        val data = DataFixture().apply { suspended = true }
        withSplash(AuthorizationFixture().apply { tokenFlow = flowOf(null) }, data) { splash, analytics, errors, directions ->
            runCurrent()
            splash.onDestroy()
            runCurrent()
            assertTrue(data.equipmentCancelled)
            assertTrue(errors.isEmpty())
            assertTrue(directions.isEmpty())
            assertEquals(listOf("auth_session_check_started", "auth_session_check_completed"), analytics.names)
        }
    }
}
