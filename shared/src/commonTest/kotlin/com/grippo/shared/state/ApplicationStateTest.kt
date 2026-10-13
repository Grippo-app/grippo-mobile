package com.grippo.shared.state

import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class ApplicationStateTest {

    @Test
    fun emptyArchiveCreatesAnIndependentDispatcher() {
        val original = ApplicationState()
        val restored = ApplicationState(original.save())

        assertNotSame(original.stateKeeper, restored.stateKeeper)
        assertNull(restored.stateKeeper.consume("missing", String.serializer()))
    }

    @Test
    fun savesTheLatestSupplierValueAndConsumesItOnce() {
        val original = ApplicationState()
        var draft = "first draft"
        original.stateKeeper.register("LoginDraft", String.serializer()) { draft }
        draft = "latest unfinished draft"

        val restored = ApplicationState(original.save())

        assertEquals(draft, restored.stateKeeper.consume("LoginDraft", String.serializer()))
        assertNull(restored.stateKeeper.consume("LoginDraft", String.serializer()))
    }

    @Test
    fun nestedComponentArchivesKeepTheirOwnKeys() {
        val original = ApplicationState()
        val login = StateKeeperDispatcher()
        val registration = StateKeeperDispatcher()
        login.register("Draft", String.serializer()) { "login email" }
        registration.register("Draft", String.serializer()) { "registration email" }
        original.stateKeeper.register("Login", SerializableContainer.serializer(), login::save)
        original.stateKeeper.register("Registration", SerializableContainer.serializer(), registration::save)

        val restored = ApplicationState(original.save())
        val restoredRegistration = StateKeeperDispatcher(
            restored.stateKeeper.consume("Registration", SerializableContainer.serializer())
        )
        val restoredLogin = StateKeeperDispatcher(
            restored.stateKeeper.consume("Login", SerializableContainer.serializer())
        )

        assertEquals("registration email", restoredRegistration.consume("Draft", String.serializer()))
        assertEquals("login email", restoredLogin.consume("Draft", String.serializer()))
    }

    @Test
    fun aNewSupplierReplacesRestoredStateForTheNextProcess() {
        val original = ApplicationState()
        original.stateKeeper.register("UserDraft", String.serializer()) { "old name" }
        val restored = ApplicationState(original.save())
        restored.stateKeeper.consume("UserDraft", String.serializer())
        restored.stateKeeper.register("UserDraft", String.serializer()) { "edited name" }

        val nextProcess = ApplicationState(restored.save())

        assertEquals("edited name", nextProcess.stateKeeper.consume("UserDraft", String.serializer()))
    }

    @Test
    fun unconstructedChildStateSurvivesAnotherCheckpoint() {
        val original = ApplicationState()
        original.stateKeeper.register("InactiveChild", String.serializer()) { "unfinished input" }

        val restored = ApplicationState(original.save())
        val nextProcess = ApplicationState(restored.save())

        assertEquals("unfinished input", nextProcess.stateKeeper.consume("InactiveChild", String.serializer()))
    }

    @Test
    fun unicodeAndEscapesSurviveTheStringBoundary() {
        val original = ApplicationState()
        val draft = "Ім’я 👩🏽‍💻\n\"quoted\" \\"
        original.stateKeeper.register("UserDraft", String.serializer()) { draft }

        val restored = ApplicationState(original.save())

        assertEquals(draft, restored.stateKeeper.consume("UserDraft", String.serializer()))
    }

    @Test
    fun invalidJSONIsRejected() {
        assertFailsWith<Exception> { ApplicationState("{broken archive") }
    }

    @Test
    fun invalidBase64IsRejected() {
        assertFailsWith<Exception> { ApplicationState("\"!invalid base64!\"") }
    }

    @Test
    fun anArchiveWithTheWrongRootSchemaIsRejected() {
        val wrongRoot = SerializableContainer("wrong root", String.serializer())
        val savedState = Json.encodeToString(SerializableContainer.serializer(), wrongRoot)

        assertFailsWith<Exception> { ApplicationState(savedState) }
    }

    @Test
    fun savePropagatesSupplierFailureWithoutReplacingTheDispatcher() {
        val state = ApplicationState()
        val failure = IllegalStateException("failed supplier")
        val keeper = state.stateKeeper
        keeper.register("Draft", String.serializer()) { throw failure }

        assertSame(failure, assertFailsWith<IllegalStateException> { state.save() })
        assertSame(keeper, state.stateKeeper)
    }
}
