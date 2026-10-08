package com.grippo.shared.dialog

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.grippo.core.state.formatters.UiText
import com.grippo.dialog.api.DialogConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class DialogRestorationTest {
    @Test
    fun savedStateDoesNotReopenASheetWithLostCallbacks() {
        var result: Float? = null
        val config = DialogConfig.WeightPicker(UiText.Str("Weight"), 70f) { result = it }
        val stateKeeper = StateKeeperDispatcher()
        val context = DefaultComponentContext(LifecycleRegistry().apply { resume() }, stateKeeper = stateKeeper)
        val navigation = SlotNavigation<DialogSession>()
        val slot = context.childSlot(
            source = navigation,
            serializer = null,
            childFactory = { _, _ -> config },
        )
        navigation.activate(DialogSession("session"))
        val savedState = stateKeeper.save()

        // Saving state must not replace the live configuration or its callback.
        assertSame(config, slot.value.child?.instance)
        (slot.value.child?.instance as DialogConfig.WeightPicker).onResult(75f)
        assertEquals(75f, result)

        val restoredContext = DefaultComponentContext(
            LifecycleRegistry().apply { resume() }, stateKeeper = StateKeeperDispatcher(savedState),
        )
        val restoredSlot = restoredContext.childSlot(
            source = SlotNavigation<DialogSession>(),
            serializer = null,
            childFactory = { _, _ -> config },
        )
        assertNull(restoredSlot.value.child)
    }

    @Test
    fun savedStateDoesNotRestoreNestedDialogSteps() {
        val root = DialogStep("root", DialogConfig.WeightPicker(UiText.Str("Weight"), 70f))
        val nested = DialogStep("nested", DialogConfig.HeightPicker(UiText.Str("Height"), 180))
        val stateKeeper = StateKeeperDispatcher()
        val context = DefaultComponentContext(LifecycleRegistry().apply { resume() }, stateKeeper = stateKeeper)
        val navigation = StackNavigation<DialogStep>()
        val stack = context.childStack(
            source = navigation,
            serializer = null,
            initialStack = { listOf(root) },
            childFactory = { step, _ -> step },
        )
        navigation.push(nested)
        val savedState = stateKeeper.save()
        assertSame(nested, stack.value.active.configuration)

        val restoredContext = DefaultComponentContext(
            LifecycleRegistry().apply { resume() }, stateKeeper = StateKeeperDispatcher(savedState),
        )
        val restoredStack = restoredContext.childStack(
            source = StackNavigation<DialogStep>(),
            serializer = null,
            initialStack = { listOf(root) },
            childFactory = { step, _ -> step },
        )
        assertSame(root, restoredStack.value.active.configuration)
        assertEquals(0, restoredStack.value.backStack.size)
    }
}
