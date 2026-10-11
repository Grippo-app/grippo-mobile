package com.grippo.core.foundation.analytics

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.grippo.toolkit.analytics.*
import com.grippo.toolkit.analytics.context.AnalyticsContext
import com.grippo.toolkit.analytics.context.EntryPoint
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.context.OperationId
import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.identity.AnalyticsIdentity
import com.grippo.toolkit.analytics.lifecycle.AnalyticsCollection
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ScreenAnalyticsTest {
    @Test fun hiddenScreensDoNotSendViewsAndEachResumeHasOnePresentation() {
        val lifecycle = LifecycleRegistry()
        val analytics = Recording()
        val flow = FlowContext.start(EntryPoint("fixture_entry"), null)
        var navigation = NavigationContext(ScreenId("fixture_screen"), reason = NavigationReason.Initial)
        lifecycle.trackScreenViews(analytics, { navigation }, { flow })
        assertEquals(0, analytics.presentations.size)
        lifecycle.resume()
        assertEquals(1, analytics.presentations.size)
        assertSame(flow, analytics.presentations[0].flow)
        lifecycle.pause()
        assertEquals(1, analytics.presentations[0].closes)
        navigation = navigation.copy(reason = NavigationReason.Restore)
        lifecycle.resume()
        assertEquals(2, analytics.presentations.size)
        assertEquals(NavigationReason.Restore, analytics.presentations[1].navigation.reason)
        lifecycle.destroy()
        assertEquals(listOf(1, 1), analytics.presentations.map { it.closes })
    }

    @Test fun bindingAnAlreadyResumedLifecycleDoesNotDuplicateTheInitialView() {
        val lifecycle = LifecycleRegistry(initialState = Lifecycle.State.RESUMED)
        val analytics = Recording()
        lifecycle.trackScreenViews(analytics, {
            NavigationContext(ScreenId("fixture_screen"), reason = NavigationReason.Restore)
        }, { null })
        assertEquals(1, analytics.presentations.size)
        lifecycle.destroy()
        assertEquals(1, analytics.presentations.single().closes)
    }

    @Test fun eachActivationUsesTheCurrentNavigationAndFlowRatherThanACapturedSnapshot() {
        val lifecycle = LifecycleRegistry()
        val analytics = Recording()
        var flow = FlowContext.start(EntryPoint("fixture_entry"), null)
        var navigation = NavigationContext(ScreenId("login"), reason = NavigationReason.Initial)
        lifecycle.trackScreenViews(analytics, { navigation }, { flow })
        lifecycle.resume(); lifecycle.pause()
        flow = FlowContext.start(EntryPoint("other_entry"), ScreenId("registration"))
        navigation = navigation.copy(referrer = ScreenId("registration"), reason = NavigationReason.Back)
        lifecycle.resume()
        assertSame(flow, analytics.presentations.last().flow)
        assertEquals(navigation, analytics.presentations.last().navigation)
        lifecycle.destroy()
    }

    @Test fun aDestroyedLifecycleRejectsBindingInsteadOfSilentlyDoingNothing() {
        val lifecycle = LifecycleRegistry(initialState = Lifecycle.State.DESTROYED)
        assertFailsWith<IllegalStateException> {
            lifecycle.trackScreenViews(Recording(), {
                NavigationContext(ScreenId("fixture_screen"), reason = NavigationReason.Initial)
            }, { null })
        }
    }

    private class Presentation(val navigation: NavigationContext, val flow: FlowContext?) : ScreenPresentation {
        var closes = 0
        override fun close() { closes++ }
    }
    private class Recording : Analytics {
        val presentations = mutableListOf<Presentation>()
        override fun presentScreen(navigation: NavigationContext, flow: FlowContext?): ScreenPresentation =
            Presentation(navigation, flow).also(presentations::add)
        override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?) =
            error("Lifecycle binding must use the atomic presentation API")
        override fun updateContext(context: AnalyticsContext) = Unit
        override fun updateIdentity(identity: AnalyticsIdentity) = Unit
        override fun updateCollection(collection: AnalyticsCollection) = Unit
    }
}
