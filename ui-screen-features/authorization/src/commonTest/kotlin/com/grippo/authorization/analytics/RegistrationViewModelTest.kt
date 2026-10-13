package com.grippo.authorization.analytics

import com.grippo.authorization.registration.RegistrationDirection
import com.grippo.authorization.registration.RegistrationLoader
import com.grippo.authorization.registration.RegistrationViewModel
import com.grippo.core.error.provider.AppError
import com.grippo.core.error.provider.ErrorProvider
import com.grippo.core.foundation.CoreModule
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.toolkit.analytics.transport.AnalyticsValue
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.OperationId
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.crashreporting.CrashReporting
import com.grippo.toolkit.crashreporting.context.CrashReportContext
import com.grippo.toolkit.crashreporting.values.CrashDiagnosticCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module

class RegistrationViewModelTest {
    @Test
    fun repeatedSubmitCannotStartAnotherRegistrationWhilePreparationIsPending() = runTest {
        for (reportingFails in listOf(false, true)) {
            val profile = CompletableDeferred<Unit>()
            val domain = AuthorizationFixture().apply {
                beforeProfile = {
                    profile.await()
                }
            }

            withViewModel(domain, reportingFails) { viewModel, recorder, errors, directions ->
                viewModel.onNextClick()
                viewModel.onNextClick()
                runCurrent()
                assertEquals(listOf("register", "profile"), domain.calls)
                assertEquals(setOf<RegistrationLoader>(RegistrationLoader.RegisterButton), viewModel.loaders.value)

                viewModel.onNextClick()
                runCurrent()
                assertEquals(listOf("register", "profile"), domain.calls)
                assertTrue(directions.isEmpty())

                profile.complete(Unit)
                advanceUntilIdle()
                assertEquals(1, domain.calls.count { it == "register" })
                assertEquals(listOf(RegistrationDirection.Home), directions)
                assertTrue(errors.isEmpty())
                assertTrue(viewModel.loaders.value.isEmpty())
                if (!reportingFails) {
                    val attempts = recorder.names.filter { it.startsWith("auth_attempt_") }
                    assertEquals(listOf("auth_attempt_started", "auth_attempt_succeeded"), attempts)
                }
            }
        }
    }

    @Test
    fun inputAndKeyboardFactsDoNotSubmitAndNeverContainCredentials() = runTest {
        val domain = AuthorizationFixture()
        withViewModel(domain) { viewModel, recorder, errors, directions ->
            recorder.events.clear()
            viewModel.onEmailChange("another@example.com")
            viewModel.onPasswordChange("AnotherPassword123!")
            assertTrue(recorder.events.isEmpty())
            viewModel.onEmailChange("private invalid email")
            viewModel.onEmailChange("another private invalid email")
            viewModel.onPasswordChange("bad")
            viewModel.onEmailFocused()
            viewModel.onPasswordFocused()
            viewModel.onPasswordVisibilityChange(true)
            viewModel.onPasswordVisibilityChange(false)
            viewModel.onEmailKeyboardNext()
            viewModel.onPasswordKeyboardDone()
            viewModel.onNextClick()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.email is EmailFormatState.Invalid)
            assertTrue(viewModel.state.value.password is PasswordFormatState.Invalid)
            assertTrue(domain.calls.isEmpty())
            assertTrue(directions.isEmpty())
            assertTrue(errors.isEmpty())
            assertEquals(
                listOf("auth_input_changed", "auth_input_changed", "auth_input_focused", "auth_input_focused",
                    "auth_password_visibility_changed", "auth_password_visibility_changed",
                    "auth_keyboard_action", "auth_keyboard_action"),
                recorder.names,
            )
            assertEquals(listOf("email" to "invalid", "password" to "invalid"), recorder.events
                .filter { it.event.schema.name == "auth_input_changed" }
                .map { it.text("input_field") to it.text("input_status") })
            assertEquals(listOf("email", "password"), recorder.events
                .filter { it.event.schema.name == "auth_input_focused" }
                .map { it.text("input_field") })
            assertEquals(listOf(AnalyticsValue.Integer(1), AnalyticsValue.Integer(0)), recorder.events
                .filter { it.event.schema.name == "auth_password_visibility_changed" }
                .map { captured -> captured.event.parameters().single { it.key.name == "password_visible" }.value })
            assertEquals(listOf("email" to "next", "password" to "done"), recorder.events
                .filter { it.event.schema.name == "auth_keyboard_action" }
                .map { it.text("input_field") to it.text("keyboard_action") })
            assertEquals(0, recorder.events.count { it.event.schema.name == "auth_submit_clicked" })
            assertTrue(recorder.names.none { it.startsWith("auth_attempt_") })

            viewModel.onEmailChange("")
            viewModel.onEmailCleared()
            assertTrue(viewModel.state.value.email is EmailFormatState.Empty)
            assertEquals(listOf("auth_input_changed", "auth_input_cleared"), recorder.names.takeLast(2))
            val clearedStatus = recorder.events.last { it.event.schema.name == "auth_input_changed" }
            assertEquals("email" to "empty", clearedStatus.text("input_field") to clearedStatus.text("input_status"))
            assertEquals("email", recorder.events.single { it.event.schema.name == "auth_input_cleared" }.text("input_field"))
            val factsAfterClear = recorder.events.size
            viewModel.onEmailChange("")
            assertEquals(factsAfterClear, recorder.events.size)
            viewModel.onEmailChange("another@example.com")
            viewModel.onPasswordChange("AnotherPassword123!")
            assertTrue(viewModel.state.value.email is EmailFormatState.Valid)
            assertTrue(viewModel.state.value.password is PasswordFormatState.Valid)
            assertEquals(listOf("auth_input_changed", "auth_input_changed"), recorder.names.takeLast(2))
            assertEquals(listOf("email" to "valid", "password" to "valid"), recorder.events.takeLast(2)
                .map { it.text("input_field") to it.text("input_status") })
            assertTrue(domain.calls.isEmpty())
            assertTrue(directions.isEmpty())
            assertTrue(recorder.names.none { it == "auth_submit_clicked" || it.startsWith("auth_attempt_") })
            assertTrue(recorder.events.all {
                it.text("interaction_screen") == "auth_registration" && it.flow == null && it.operation == null
            })
            assertTrue(recorder.events.flatMap { it.event.parameters() }.all { parameter ->
                val value = parameter.value
                value !is AnalyticsValue.Text || value.value in setOf(
                    "auth_registration", "email", "password", "invalid", "empty", "valid", "next", "done",
                )
            })
        }
    }

    private fun TestScope.withViewModel(
        domain: AuthorizationFixture,
        reportingFails: Boolean = false,
        verify: TestScope.(RegistrationViewModel, RecordingAuthAnalytics, List<Throwable>, List<RegistrationDirection>) -> Unit,
    ) {
        val errors = mutableListOf<Throwable>()
        val directions = mutableListOf<RegistrationDirection>()
        val recorder = RecordingAuthAnalytics()
        val observer: Analytics = if (reportingFails) object : Analytics by recorder {
            override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?) {
                throw IllegalStateException("reporting defect")
            }
        } else recorder
        val analytics = factory(observer)
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
        var viewModel: RegistrationViewModel? = null
        try {
            val registration = RegistrationViewModel(analytics, domain.registerUseCase())
            viewModel = registration
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                registration.navigator.collect { directions += it }
            }
            registration.onEmailChange("person@example.com")
            registration.onPasswordChange("Password123!")
            verify(registration, recorder, errors, directions)
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
                    assertEquals(listOf(if (profile) RegistrationDirection.Home else RegistrationDirection.CreateProfile), directions)
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
                    assertTrue(attempts.last().event.parameters().none { it.key.name.startsWith("failure_") })
                    assertEquals(attempts.first().operation, attempts.last().operation)
                }
            }
        }
    }
}
