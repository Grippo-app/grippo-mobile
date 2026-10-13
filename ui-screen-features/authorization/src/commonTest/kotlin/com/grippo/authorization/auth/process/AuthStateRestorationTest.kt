package com.grippo.authorization.auth.process

import com.grippo.authorization.login.LoginState
import com.grippo.authorization.registration.RegistrationLoader
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class AuthStateRestorationTest {
    @Test
    fun registrationRestoresEditedInvalidEmailInsteadOfRoutePrefillAndDropsPassword() = runTest {
        withAuthStateRestorationFixture { fixture ->
            val original = fixture.model()
            assertEquals(AuthStateRestorationFixture.ROUTE_EMAIL, original.state.value.email.value)
            val views = fixture.recorder.views.size
            original.onEmailChange("edited invalid email")
            original.onPasswordChange("DummySecretBeforeRestore123!")
            original.onNextClick()
            runCurrent()
            val email = original.state.value.email
            assertIs<EmailFormatState.Invalid>(email)
            assertSame(original, fixture.model())
            assertEquals(views, fixture.recorder.views.size)
            val events = fixture.recorder.names

            fixture.restore("DummySecretBeforeRestore123!")
            runCurrent()
            val restored = fixture.model()
            assertNotSame(original, restored)
            assertEquals(email, restored.state.value.email)
            assertEquals(PasswordFormatState.Empty(), restored.state.value.password)
            assertEquals(events, fixture.recorder.names)
            assertEquals(listOf("auth_registration", "auth_registration"), fixture.recorder.views
                .map { it.navigation.screen.analyticsCode })
            assertTrue(fixture.recorder.views.all { it.navigation.reason == NavigationReason.Initial })
            restored.onNextClick()
            runCurrent()
            assertEquals(events, fixture.recorder.names)
            assertTrue(fixture.domain.calls.isEmpty())
            restored.onBack()
            runCurrent()
            assertEquals(1, fixture.backNavigations)
            assertEquals(0, fixture.homeNavigations)
            assertEquals(0, fixture.profileNavigations)
        }
    }

    @Test
    fun explicitlyClearedRegistrationEmailSurvivesRestoreInsteadOfValidRouteSeed() = runTest {
        withAuthStateRestorationFixture { fixture ->
            val original = fixture.model()
            val views = fixture.recorder.views.size
            original.onEmailChange("")
            original.onEmailCleared()
            original.onPasswordChange("DummySecretForClearedEmail123!")
            assertSame(original, fixture.model())
            assertEquals(views, fixture.recorder.views.size)
            val events = fixture.recorder.names

            fixture.restore("DummySecretForClearedEmail123!")
            runCurrent()
            val restored = fixture.model()
            assertNotSame(original, restored)
            assertEquals(EmailFormatState.Empty(), restored.state.value.email)
            assertEquals(PasswordFormatState.Empty(), restored.state.value.password)
            assertEquals(events, fixture.recorder.names)
            restored.onNextClick()
            restored.onPasswordChange("ReenteredDummyPassword123!")
            val afterPassword = fixture.recorder.names
            restored.onNextClick()
            runCurrent()
            assertEquals(afterPassword, fixture.recorder.names)
            assertTrue(fixture.domain.calls.isEmpty())
            assertTrue(restored.loaders.value.isEmpty())
            assertEquals(0, fixture.profileNavigations)
            assertEquals(0, fixture.homeNavigations)
        }
    }

    @Test
    fun restoreCancelsRegistrationWithoutReplayAndNewPasswordStartsANewWholeOperation() = runTest {
        withAuthStateRestorationFixture { fixture ->
            val original = fixture.model()
            original.onEmailChange("edited-registration@example.com")
            original.onPasswordChange("DummyOldPassword123!")
            original.onNextClick()
            runCurrent()
            assertEquals(listOf("register", "profile"), fixture.domain.calls)
            val oldJob = fixture.businessJobs.single()
            assertTrue(oldJob.isActive)

            fixture.restore("DummyOldPassword123!")
            runCurrent()
            val restored = fixture.model()
            assertNotSame(original, restored)
            assertTrue(oldJob.isCompleted)
            assertTrue(original.loaders.value.isEmpty())
            assertEquals("edited-registration@example.com", restored.state.value.email.value)
            assertEquals(PasswordFormatState.Empty(), restored.state.value.password)
            assertTrue(restored.loaders.value.isEmpty())
            val calls = fixture.domain.calls.toList()
            val events = fixture.recorder.names
            restored.onNextClick()
            runCurrent()
            assertEquals(calls, fixture.domain.calls)
            assertEquals(events, fixture.recorder.names)
            assertEquals(1, fixture.businessJobs.size)
            assertEquals(0, fixture.profileNavigations)
            restored.onPasswordChange("DummyNewPassword123!")
            restored.onNextClick()
            runCurrent()
            val newJob = fixture.businessJobs.last()
            assertNotSame(oldJob, newJob)
            assertTrue(newJob.isActive)
            assertEquals(setOf<RegistrationLoader>(RegistrationLoader.RegisterButton), restored.loaders.value)
            assertEquals("edited-registration@example.com", fixture.domain.credential)
            restored.onNextClick()
            runCurrent()
            assertEquals(2, fixture.domain.calls.count { it == "register" })
            fixture.pendingProfile.complete(Unit)
            runCurrent()
            assertTrue(newJob.isCompleted)
            assertTrue(restored.loaders.value.isEmpty())
            assertEquals(listOf("register", "profile", "register", "profile", "push_fetch", "push_register"), fixture.domain.calls)
            assertEquals(1, fixture.profileNavigations)
            assertEquals(0, fixture.homeNavigations)
            val attempts = fixture.recorder.events.filter { it.event.schema.name.startsWith("auth_attempt_") }
            assertEquals(listOf("auth_attempt_started", "auth_attempt_interrupted", "auth_attempt_started", "auth_attempt_succeeded"),
                attempts.map { it.event.schema.name })
            assertNotNull(attempts[0].operation)
            assertEquals(attempts[0].operation, attempts[1].operation)
            assertNotEquals(attempts[0].operation, attempts[2].operation)
            assertEquals(attempts[2].operation, attempts[3].operation)
        }
    }

    @Test
    fun loginStateSerializationKeepsExactEmailAndOmitsPasswordAndProviderAvailability() {
        for (email in listOf("invalid current email", "edited-login@example.com", "")) {
            val input = EmailFormatState.of(email)
            val state = LoginState(
                email = input,
                password = PasswordFormatState.of("DummyPasswordMustNotPersist123!"),
                isGoogleLoginAvailable = true,
                isAppleLoginAvailable = true,
            )
            val archive = Json.encodeToString(state)
            val payload = Json.parseToJsonElement(archive).jsonObject
            assertFalse("password" in payload)
            assertFalse("isGoogleLoginAvailable" in payload)
            assertFalse("isAppleLoginAvailable" in payload)
            assertFalse("DummyPasswordMustNotPersist123!" in archive)
            val restored = Json.decodeFromString<LoginState>(archive)
            assertEquals(input, restored.email)
            assertEquals(PasswordFormatState.Empty(), restored.password)
            assertFalse(restored.isGoogleLoginAvailable)
            assertFalse(restored.isAppleLoginAvailable)
        }
    }
}
