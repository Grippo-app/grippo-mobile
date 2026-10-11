package com.grippo.toolkit.analytics.core.internal

import com.grippo.toolkit.analytics.*
import com.grippo.toolkit.analytics.context.AnalyticsContext
import com.grippo.toolkit.analytics.context.EntryPoint
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.FlowId
import com.grippo.toolkit.analytics.context.LaunchSource
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.events.ScreenViewed
import com.grippo.toolkit.analytics.identity.AnalyticsIdentity
import com.grippo.toolkit.analytics.identity.AnalyticsUserId
import com.grippo.toolkit.analytics.lifecycle.AnalyticsCollection
import com.grippo.toolkit.analytics.transport.AnalyticsIdentityPayload
import com.grippo.toolkit.analytics.transport.AnalyticsPayload
import com.grippo.toolkit.analytics.transport.AnalyticsSink
import com.grippo.toolkit.analytics.transport.AnalyticsValue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*
import kotlinx.coroutines.*

class AnalyticsConcurrencyTest {
    @Test fun concurrentPresentationsCannotMixScreenAndNavigationContext() = runBlocking {
        val worker = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val producers = Executors.newFixedThreadPool(4)
        val count = 200
        val complete = CountDownLatch(count)
        val received = java.util.Collections.synchronizedList(mutableListOf<AnalyticsPayload>())
        val core = DefaultAnalytics(dispatcher = worker, queueCapacity = count)
        val sink = object : AnalyticsSink {
            override fun setCollection(collection: AnalyticsCollection) = Unit
            override fun setIdentity(identity: AnalyticsIdentityPayload) = Unit
            override fun send(payload: AnalyticsPayload) { received.add(payload); complete.countDown() }
        }
        try {
            core.updateContext(AnalyticsContext(launchSource = LaunchSource.Notification))
            core.attachSink(sink); core.updateCollection(AnalyticsCollection.Granted)
            val jobs = List(count) { index ->
                producers.submit {
                    val screen = ScreenId("screen_$index")
                    core.presentScreen(NavigationContext(screen, reason = NavigationReason.Action),
                        FlowContext(FlowId("flow_$index"), EntryPoint("entry_$index"), screen))
                }
            }
            jobs.forEach { it.get(10, TimeUnit.SECONDS) }
            assertTrue(complete.await(10, TimeUnit.SECONDS))
            assertEquals(count, received.size)
            received.forEach {
                assertEquals(it.parameters["screen_name"], it.parameters["screen_id"])
                assertEquals(it.parameters["screen_name"], it.parameters["origin_screen"])
                assertEquals(AnalyticsValue.Text("notification"), it.parameters["launch_source"])
            }
        } finally {
            core.close()
            producers.shutdownNow()
            worker.close()
        }
    }

    @Test fun immediateDispatcherNeverCallsSinkWhileHoldingTheStateLock() = runBlocking {
        val observer = Executors.newSingleThreadExecutor()
        val core = DefaultAnalytics(dispatcher = Dispatchers.Unconfined)
        var observedOutsideLock = false
        val sink = object : AnalyticsSink {
            override fun setCollection(collection: AnalyticsCollection) {
                val observed = CountDownLatch(1)
                observer.submit { core.updateContext(AnalyticsContext()); observed.countDown() }
                observedOutsideLock = observed.await(1, TimeUnit.SECONDS)
            }
            override fun setIdentity(identity: AnalyticsIdentityPayload) = Unit
            override fun send(payload: AnalyticsPayload) = Unit
        }
        try {
            core.attachSink(sink)
            assertTrue(observedOutsideLock, "SDK callback ran under the core state lock")
        } finally {
            core.close()
            observer.shutdownNow()
            assertTrue(observer.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    @Test fun identityRacesAccountForEveryQueuedEvent() = runBlocking {
        val worker = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val producers = Executors.newFixedThreadPool(2)
        val count = 2_000
        val start = CountDownLatch(1)
        val core = DefaultAnalytics(dispatcher = worker, queueCapacity = count, diagnosticCapacity = count * 3)
        val sink = object : AnalyticsSink {
            override fun setCollection(collection: AnalyticsCollection) = Unit
            override fun setIdentity(identity: AnalyticsIdentityPayload) = Unit
            override fun send(payload: AnalyticsPayload) = Unit
        }
        try {
            core.attachSink(sink)
            core.updateCollection(AnalyticsCollection.Granted)
            val events = producers.submit {
                start.await()
                repeat(count) { core.track(ScreenViewed(ScreenId("screen_$it"))) }
            }
            val identities = producers.submit {
                start.await()
                repeat(count) { core.updateIdentity(AnalyticsIdentity(AnalyticsUserId("user_${it % 2}"))) }
            }
            start.countDown()
            events.get(10, TimeUnit.SECONDS)
            identities.get(10, TimeUnit.SECONDS)
            withTimeout(10_000) { while (core.queuedEventCount() != 0) delay(1) }
            // The single-thread fence waits for a dequeued SDK call and its diagnostic to finish.
            withContext(worker) { }
            val journal = core.diagnostics()
            assertEquals(count, journal.count { it.disposition == AnalyticsDisposition.Queued })
            val terminal = setOf(AnalyticsDisposition.AcceptedBySink,
                AnalyticsDisposition.DiscardedOnIdentityChange, AnalyticsDisposition.DiscardedBeforeSend)
            assertEquals(count, journal.count { it.disposition in terminal })
        } finally {
            core.close()
            producers.shutdownNow()
            worker.close()
        }
    }

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
