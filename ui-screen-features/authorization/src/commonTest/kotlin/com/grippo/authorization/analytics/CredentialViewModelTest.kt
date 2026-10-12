package com.grippo.authorization.analytics

import com.grippo.analytics.authorization.models.EntryPoint
import com.grippo.authorization.registration.credential.CredentialDirection
import com.grippo.authorization.registration.credential.CredentialViewModel
import com.grippo.core.error.provider.AppError
import com.grippo.core.error.provider.ErrorProvider
import com.grippo.core.foundation.CoreModule
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.FlowId
import com.grippo.toolkit.analytics.context.OperationId
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.crashreporting.CrashReporting
import com.grippo.toolkit.crashreporting.context.CrashReportContext
import com.grippo.toolkit.crashreporting.values.CrashDiagnosticCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module

class CredentialViewModelTest {
    private fun TestScope.withViewModel(
        domain: AuthorizationFixture,
        reportingFails: Boolean = false,
        verify: TestScope.(CredentialViewModel, RecordingAuthAnalytics, List<Throwable>, List<CredentialDirection>) -> Unit,
    ) {
        val errors = mutableListOf<Throwable>()
        val directions = mutableListOf<CredentialDirection>()
        val recorder = RecordingAuthAnalytics()
        val observer: Analytics = if (reportingFails) object : Analytics by recorder {
            override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?) {
                throw IllegalStateException("reporting defect")
            }
        } else recorder
        val scenario = factory(observer).authorization(EntryPoint.TokenAbsent, FlowId.create())
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        startKoin {
            modules(CoreModule().module, module {
                single<ErrorProvider> { object : ErrorProvider {
                    override suspend fun provide(exception: Throwable, callback: () -> Unit) {
                        errors += exception
                    }
                } }
                single<CrashReporting> { object : CrashReporting {
                    override fun reportException(throwable: Throwable, context: CrashReportContext) = Unit
                    override fun logBreadcrumb(code: CrashDiagnosticCode) = Unit
                } }
            })
        }
        var viewModel: CredentialViewModel? = null
        try {
            val credential = CredentialViewModel(null, scenario, domain.registerUseCase())
            viewModel = credential
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                credential.navigator.collect { directions += it }
            }
            credential.onEmailChange("person@example.com")
            credential.onPasswordChange("Password123!")
            verify(credential, recorder, errors, directions)
        } finally {
            viewModel?.onDestroy()
            stopKoin()
            Dispatchers.resetMain()
        }
    }

    @Test fun wholeRegistrationDrivesNavigationEvenWhenReportingFails() = runTest {
        for (profile in listOf(true, false)) {
            for (reportingFails in listOf(true, false)) {
                val domain = AuthorizationFixture().apply { hasProfile = profile }
                withViewModel(domain, reportingFails) { viewModel, recorder, errors, directions ->
                    viewModel.onNextClick()
                    advanceUntilIdle()
                    assertEquals(listOf(if (profile) CredentialDirection.Home else CredentialDirection.CreateProfile), directions)
                    assertEquals("register", domain.calls.first())
                    assertEquals("push_register", domain.calls.last())
                    assertTrue(errors.isEmpty())
                    assertTrue(viewModel.loaders.value.isEmpty())
                    if (!reportingFails) {
                        val attempts = recorder.events.filter { it.event.schema.name.startsWith("auth_attempt_") }
                        assertEquals(listOf("auth_attempt_started", "auth_attempt_succeeded"), attempts.map { it.event.schema.name })
                        assertTrue(attempts.all { it.text("auth_action") == "register" && it.text("auth_method") == "email" })
                        assertEquals(attempts.first().operation, attempts.last().operation)
                    }
                }
            }
        }
    }

    @Test fun preparationFailureUsesExistingErrorPipelineAndNeverNavigates() = runTest {
        for (reportingFails in listOf(true, false)) {
            val original = AppError.Network.Timeout("private")
            val domain = AuthorizationFixture().apply { profileError = original }
            withViewModel(domain, reportingFails) { viewModel, recorder, errors, directions ->
                viewModel.onNextClick()
                advanceUntilIdle()
                val reported = errors.single()
                assertSame(original, if (reported.cause === original) reported.cause else reported)
                assertEquals(listOf("register", "profile"), domain.calls)
                assertTrue(directions.isEmpty())
                assertTrue(viewModel.loaders.value.isEmpty())
                if (!reportingFails) {
                    val attempts = recorder.events.filter { it.event.schema.name.startsWith("auth_attempt_") }
                    assertEquals(listOf("auth_attempt_started", "auth_attempt_failed"), attempts.map { it.event.schema.name })
                    assertEquals("register", attempts.last().text("auth_action"))
                    assertEquals("timeout", attempts.last().text("failure_category"))
                    assertEquals(attempts.first().operation, attempts.last().operation)
                }
            }
        }
    }
}
