package com.grippo.toolkit.analytics.core.internal

import com.grippo.toolkit.analytics.AnalyticsCollection
import com.grippo.toolkit.analytics.EntryPoint
import com.grippo.toolkit.analytics.FlowContext
import com.grippo.toolkit.analytics.FlowId
import com.grippo.toolkit.analytics.ScreenId
import com.grippo.toolkit.analytics.ScreenViewed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class AnalyticsParallelTest {
    @Test fun parallelProducersPreserveIndependentFlowsOnBothPlatforms() = runBlocking {
        val count = 200
        val core = DefaultAnalytics(dispatcher = Dispatchers.Default, queueCapacity = count)
        val sink = RecordingAnalyticsSink(capacity = count)
        try {
            core.attachSink(sink)
            core.updateCollection(AnalyticsCollection.Granted)
            coroutineScope {
                repeat(4) { producer ->
                    launch(Dispatchers.Default) {
                        repeat(50) { action ->
                            val index = producer * 50 + action
                            val screen = ScreenId("screen_$index")
                            core.track(ScreenViewed(screen), FlowContext(
                                FlowId("flow_$index"), EntryPoint("entry_$index"), screen,
                            ))
                        }
                    }
                }
            }
            withTimeout(10_000) { while (sink.payloads().size != count) delay(1) }
            val payloads = sink.payloads()
            assertEquals(count, payloads.map { it.parameters["flow_id"] }.distinct().size)
            payloads.forEach { assertEquals(it.parameters["screen_name"], it.parameters["origin_screen"]) }
        } finally {
            core.close()
        }
    }
}
