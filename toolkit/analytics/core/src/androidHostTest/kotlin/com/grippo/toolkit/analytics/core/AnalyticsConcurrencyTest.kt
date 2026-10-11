package com.grippo.toolkit.analytics.core

import com.grippo.toolkit.analytics.*
import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

class AnalyticsConcurrencyTest {
    @Test fun concurrentProducersDoNotLoseEventsAndNeverCallSinkConcurrently() = runBlocking {
        val worker = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val producers = Executors.newFixedThreadPool(4)
        val count = 200
        val complete = CountDownLatch(count)
        val received = java.util.Collections.synchronizedList(mutableListOf<AnalyticsPayload>())
        val active = java.util.concurrent.atomic.AtomicInteger(0)
        val overlap = java.util.concurrent.atomic.AtomicBoolean(false)
        val core = DefaultAnalytics(dispatcher = worker, queueCapacity = count)
        val sink = object : AnalyticsSink {
            override fun setCollection(collection: AnalyticsCollection) = Unit
            override fun setIdentity(identity: AnalyticsIdentityPayload) = Unit
            override fun send(payload: AnalyticsPayload) {
                if (active.incrementAndGet() != 1) overlap.set(true)
                received.add(payload)
                active.decrementAndGet()
                complete.countDown()
            }
        }
        try {
            core.attachSink(sink); core.updateCollection(AnalyticsCollection.Granted)
            repeat(count) { index ->
                producers.submit {
                    val screen = ScreenId("screen_$index")
                    core.track(ScreenViewed(screen), FlowContext(FlowId("flow_$index"), EntryPoint("entry_$index"), screen))
                }
            }
            producers.shutdown()
            assertTrue(producers.awaitTermination(10, TimeUnit.SECONDS))
            assertTrue(complete.await(10, TimeUnit.SECONDS))
            assertFalse(overlap.get())
            assertEquals(count, received.size)
            assertEquals(count, received.map { it.parameters["flow_id"] }.distinct().size)
            received.forEach { assertEquals(it.parameters["screen_name"], it.parameters["origin_screen"]) }
        } finally {
            core.close()
            producers.shutdownNow()
            worker.close()
        }
    }
}
