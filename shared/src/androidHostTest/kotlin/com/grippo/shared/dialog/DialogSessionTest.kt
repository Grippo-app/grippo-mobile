package com.grippo.shared.dialog

import com.grippo.core.state.formatters.UiText
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController.Session
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DialogSessionTest {
    private val root = DialogConfig.WeightPicker(title = UiText.Str("Weight"), initial = 70f)
    private val next = DialogConfig.HeightPicker(title = UiText.Str("Height"), initial = 180)
    private val session = DialogSession("session-1")
    private var state = DialogState(
        session = session,
        stack = persistentListOf(DialogEntry(root)),
        phase = SheetPhase.Present,
    )

    private fun navigation(owner: DialogConfig = root): Session = SessionDialogNavigation(
        sessionId = session.id,
        ownerId = state.stack.first { it.config === owner }.id,
        currentState = { state },
        updateState = { transform -> state = transform(state) },
        onFinish = { result ->
            state = if (state.stack.size == 1) state.copy(phase = SheetPhase.Dismissing)
            else state.copy(stack = state.stack.removeAt(state.stack.lastIndex))
            result?.invoke()
        },
        onClose = { state = state.copy(phase = SheetPhase.Dismissing) },
    )

    @Test
    fun pushKeepsTheSheetAndPreviousStep() {
        navigation().push(next)
        assertSame(session, state.session)
        assertEquals(listOf(root, next), state.stack.map { it.config })
        navigation(next).back()
        assertEquals(listOf(root), state.stack.map { it.config })
        assertEquals(SheetPhase.Present, state.phase)
    }

    @Test
    fun replacingRootKeepsTheSameSheetSession() {
        navigation().replaceCurrent(next)
        assertSame(session, state.session)
        assertEquals(listOf(next), state.stack.map { it.config })
        assertEquals(SheetPhase.Present, state.phase)
    }

    @Test
    fun replacingSameContentCanInstallANewResultCallback() {
        var selected: Float? = null
        val replacement = root.copy(onResult = { selected = it })
        val previousId = state.stack.single().id
        navigation().replaceCurrent(replacement)
        assertSame(session, state.session)
        assertEquals(false, previousId == state.stack.single().id)
        (state.stack.single().config as DialogConfig.WeightPicker).onResult(75f)
        assertEquals(75f, selected)
    }

    @Test
    fun replacingNestedStepKeepsEarlierSteps() {
        navigation().push(next)
        val replacement = DialogConfig.DurationPicker(UiText.Str("Duration"), null)
        navigation(next).replaceCurrent(replacement)
        assertEquals(listOf(root, replacement), state.stack.map { it.config })
    }

    @Test
    fun replacedStepCannotNavigate() {
        val old = navigation()
        old.replaceCurrent(next)
        val before = state
        old.push(root)
        old.back()
        old.close()
        assertEquals(before, state)
    }

    @Test
    fun removedStepStaysInvalidWhenIdenticalContentIsOpenedAgain() {
        val old = navigation()
        old.replaceCurrent(next)
        navigation(next).replaceCurrent(root)
        val before = state
        old.push(next)
        old.close()
        assertEquals(before, state)
    }

    @Test
    fun oldSessionCannotChangeANewSessionWithIdenticalContent() {
        val old = navigation()
        state = state.copy(session = DialogSession("session-2"))
        val before = state
        old.replaceCurrent(next)
        old.close()
        assertEquals(before, state)
    }

    @Test
    fun suspendedStepCannotNavigateUntilBackResumesIt() {
        val parent = navigation()
        parent.push(next)
        val before = state
        parent.replaceCurrent(root)
        parent.back()
        parent.close()
        assertEquals(before, state)
        navigation(next).back()
        parent.push(next)
        assertEquals(listOf(root, next), state.stack.map { it.config })
    }

    @Test
    fun rootBackStartsClosingAndClosingSessionIgnoresNavigation() {
        val navigation = navigation()
        navigation.back()
        assertEquals(SheetPhase.Dismissing, state.phase)
        val before = state
        navigation.push(next)
        navigation.replaceCurrent(next)
        assertEquals(before, state)
    }

    @Test
    fun duplicatePushDoesNotReopenTheCurrentStep() {
        navigation().push(root)
        assertEquals(listOf(root), state.stack.map { it.config })
    }

    @Test
    fun sameContentWithADifferentTitleIsADistinctStep() {
        val renamed = root.copy(title = UiText.Str("Body weight"))
        navigation().push(renamed)
        assertEquals(listOf(root, renamed), state.stack.map { it.config })
    }

    @Test
    fun closeFromNestedStepClosesTheWholeSession() {
        navigation().push(next)
        navigation(next).close()
        assertEquals(SheetPhase.Dismissing, state.phase)
        assertEquals(listOf(root, next), state.stack.map { it.config })
    }

    @Test
    fun delayedOpeningCallbackRemainsBoundToItsOriginatingStep() {
        val origin = navigation()
        val openDialog: (DialogConfig) -> Unit = origin::push
        origin.replaceCurrent(next)
        navigation(next).replaceCurrent(root)
        val before = state
        openDialog(next)
        assertEquals(before, state)
    }

    @Test
    fun aResultCallbackCanContinueFromTheResumedParentStep() {
        navigation().push(next)
        val continuation = DialogConfig.DurationPicker(UiText.Str("Duration"), null)
        (navigation(next) as SessionDialogNavigation).finish {
            navigation(root).push(continuation)
        }
        assertEquals(listOf(root, continuation), state.stack.map { it.config })
    }

    @Test
    fun removedStepCannotDeliverALateResult() {
        val old = navigation() as SessionDialogNavigation
        old.replaceCurrent(next)
        var delivered = false
        old.finish { delivered = true }
        assertEquals(false, delivered)
        assertEquals(listOf(next), state.stack.map { it.config })
    }


}
