package com.grippo.toolkit.analytics.core.internal

import com.grippo.toolkit.analytics.*
import com.grippo.toolkit.analytics.context.AnalyticsContext
import com.grippo.toolkit.analytics.context.EntryPoint
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.LaunchSource
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.lifecycle.AnalyticsCollection
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import com.grippo.toolkit.analytics.transport.AnalyticsValue
import kotlin.test.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class ScreenPresentationTest {
    private class Action : AnalyticsEvent {
        override val schema = Schema
        override fun parameters(): List<AnalyticsParameter> = emptyList()
        companion object { val Schema = AnalyticsEventSchema("fixture_action", 1, emptyList()) }
    }
    private fun core(dispatcher: kotlinx.coroutines.CoroutineDispatcher): DefaultAnalytics = DefaultAnalytics(
        AnalyticsCatalog.fromSchemas(listOf(AnalyticsSchema(listOf(Action.Schema)))), dispatcher,
    )
    private fun screen(name: String, reason: NavigationReason = NavigationReason.Action) =
        NavigationContext(ScreenId(name), ScreenId("origin"), reason)

    @Test fun screenAndContextAreCapturedTogetherAndLaunchSourceIsPreserved() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val sink = RecordingAnalyticsSink()
        core.updateContext(AnalyticsContext(launchSource = LaunchSource.Notification))
        core.updateCollection(AnalyticsCollection.Granted)
        core.attachSink(sink)
        val flow = FlowContext.start(EntryPoint("fixture_entry"), ScreenId("origin"))
        val first = core.presentScreen(screen("first"), flow)
        val second = core.presentScreen(screen("second", NavigationReason.Back), flow)
        runCurrent()
        val payloads = sink.payloads()
        assertEquals(2, payloads.size)
        payloads.forEach { payload ->
            assertEquals("screen_view", payload.name)
            assertEquals(payload.parameters["screen_name"], payload.parameters["screen_id"])
            assertEquals(AnalyticsValue.Text("notification"), payload.parameters["launch_source"])
            assertEquals(AnalyticsValue.Text(flow.id.value), payload.parameters["flow_id"])
            assertEquals(AnalyticsValue.Text("origin"), payload.parameters["referrer_screen"])
        }
        assertEquals(AnalyticsValue.Text("back"), payloads[1].parameters["navigation_reason"])
        first.close(); second.close(); core.close()
    }

    @Test fun closingAnOlderPresentationCannotClearANewerOneEvenForTheSameScreen() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val sink = RecordingAnalyticsSink()
        core.updateCollection(AnalyticsCollection.Granted); core.attachSink(sink)
        val first = core.presentScreen(screen("same"))
        val second = core.presentScreen(screen("same", NavigationReason.Restore))
        first.close(); first.close()
        core.track(Action())
        second.close(); second.close()
        core.track(Action())
        runCurrent()
        val actions = sink.payloads().filter { it.name == "fixture_action" }
        assertEquals(AnalyticsValue.Text("same"), actions[0].parameters["screen_id"])
        assertEquals(AnalyticsValue.Text("restore"), actions[0].parameters["navigation_reason"])
        assertFalse("screen_id" in actions[1].parameters)
        core.close()
    }

    @Test fun anExplicitContextUpdateInvalidatesThePreviousPresentation() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val sink = RecordingAnalyticsSink()
        core.updateCollection(AnalyticsCollection.Granted); core.attachSink(sink)
        val presentation = core.presentScreen(screen("first"))
        core.updateContext(AnalyticsContext(screen("external"), LaunchSource.DeepLink))
        presentation.close()
        core.track(Action()); runCurrent()
        val action = sink.payloads().last()
        assertEquals(AnalyticsValue.Text("external"), action.parameters["screen_id"])
        assertEquals(AnalyticsValue.Text("deep_link"), action.parameters["launch_source"])
        core.close()
    }

    @Test fun presentationsWithoutConsentAreNotReplayedWhenConsentIsGranted() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val sink = RecordingAnalyticsSink()
        core.attachSink(sink)
        val presentation = core.presentScreen(screen("visible"))
        core.updateCollection(AnalyticsCollection.Granted); runCurrent()
        assertEquals(emptyList(), sink.payloads())
        core.track(Action()); runCurrent()
        assertEquals(AnalyticsValue.Text("visible"), sink.payloads().single().parameters["screen_id"])
        presentation.close(); core.close()
    }

    @Test fun closingTheServiceMakesOutstandingPresentationsHarmless() = runTest {
        val core = core(StandardTestDispatcher(testScheduler))
        val sink = RecordingAnalyticsSink()
        core.attachSink(sink)
        val presentation = core.presentScreen(screen("first"))
        core.close()
        presentation.close()
        core.presentScreen(screen("closed")).close()
        assertEquals(emptyList(), sink.payloads())
        assertEquals(0, core.queuedEventCount())
    }
}
