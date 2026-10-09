package com.grippo.main

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.grippo.screen.api.MainRouter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MainTabNavigationTest {
    private class Child(val lifecycle: Lifecycle, var selectedDate: Int = 1)

    @Test
    fun switchingTabsRetainsTheirStateAndOnlyResumesTheSelectedChild() {
        val context = DefaultComponentContext(LifecycleRegistry().apply { resume() })
        val navigation = MainTabNavigation(context) { _, childContext -> Child(childContext.lifecycle) }
        val home = navigation.childStack.value.active.instance
        navigation.select(MainRouter.Calendar)
        val calendar = navigation.childStack.value.active.instance
        calendar.selectedDate = 9
        navigation.select(MainRouter.Profile)
        assertEquals(Lifecycle.State.CREATED, calendar.lifecycle.state)
        navigation.select(MainRouter.Home)
        assertSame(home, navigation.childStack.value.active.instance)
        navigation.select(MainRouter.Calendar)
        assertSame(calendar, navigation.childStack.value.active.instance)
        assertEquals(9, navigation.childStack.value.active.instance.selectedDate)
        assertEquals(Lifecycle.State.RESUMED, calendar.lifecycle.state)
    }

    @Test
    fun reselectingATabDoesNotCreateAnotherChild() {
        var creations = 0
        val context = DefaultComponentContext(LifecycleRegistry().apply { resume() })
        val navigation = MainTabNavigation(context) { _, _ -> creations++; Any() }
        navigation.select(MainRouter.Calendar)
        repeat(3) { navigation.select(MainRouter.Calendar) }
        assertEquals(2, creations)
        assertEquals(1, navigation.childStack.value.backStack.size)
    }

    @Test
    fun backReturnsToHomeAndThenDelegatesToTheHost() {
        val context = DefaultComponentContext(LifecycleRegistry().apply { resume() })
        val navigation = MainTabNavigation(context) { tab, _ -> tab }
        navigation.select(MainRouter.Calendar)
        navigation.select(MainRouter.Profile)
        assertTrue(navigation.back())
        assertEquals(MainRouter.Home, navigation.childStack.value.active.configuration)
        assertFalse(navigation.back())
        navigation.select(MainRouter.Calendar)
        assertTrue(navigation.back())
        assertEquals(MainRouter.Home, navigation.childStack.value.active.configuration)
    }

    @Test
    fun restorationKeepsTheSelectedTabAndBackStillReturnsToHome() {
        val keeper = StateKeeperDispatcher()
        val context = DefaultComponentContext(LifecycleRegistry().apply { resume() }, stateKeeper = keeper)
        val navigation = MainTabNavigation(context) { tab, _ -> tab }
        navigation.select(MainRouter.Calendar)
        navigation.select(MainRouter.Profile)
        val restoredContext = DefaultComponentContext(
            LifecycleRegistry().apply { resume() }, stateKeeper = StateKeeperDispatcher(keeper.save()),
        )
        val restored = MainTabNavigation(restoredContext) { tab, _ -> tab }
        assertEquals(MainRouter.Profile, restored.childStack.value.active.configuration)
        assertEquals(listOf(MainRouter.Home, MainRouter.Calendar), restored.childStack.value.backStack.map { it.configuration })
        assertTrue(restored.back())
        assertEquals(MainRouter.Home, restored.childStack.value.active.configuration)
    }
}
