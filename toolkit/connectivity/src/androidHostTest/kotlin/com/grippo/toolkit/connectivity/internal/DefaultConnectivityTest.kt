package com.grippo.toolkit.connectivity.internal

import com.grippo.toolkit.connectivity.Connectivity
import com.grippo.toolkit.connectivity.ConnectivityOptions
import com.grippo.toolkit.connectivity.ConnectivityProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DefaultConnectivityTest {
    @Test
    fun concurrentStartsAndStopsLeaveNoMonitorRunning() = runTest {
        val owner = SupervisorJob()
        val scope = CoroutineScope(owner + Dispatchers.Default)
        val provider = object : ConnectivityProvider {
            override fun monitor() = flow<Connectivity.Status> { awaitCancellation() }
        }
        val connectivity = DefaultConnectivity(scope, provider, ConnectivityOptions())
        val executor = Executors.newFixedThreadPool(4)
        try {
            val workers = List(4) {
                executor.submit {
                    repeat(200) {
                        connectivity.start()
                        connectivity.stop()
                    }
                }
            }
            workers.forEach { it.get(10, TimeUnit.SECONDS) }
            connectivity.stop()
            withContext(Dispatchers.Default) {
                withTimeout(5_000) { owner.children.toList().joinAll() }
            }
            assertFalse(connectivity.isMonitoring)
            assertEquals(0, owner.children.count())
        } finally {
            executor.shutdownNow()
            scope.cancel()
        }
    }

    @Test
    fun restartDuringOwnerCancellationDoesNotRemainActive() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val provider = object : ConnectivityProvider {
            override fun monitor() = flow<Connectivity.Status> { awaitCancellation() }
        }
        val connectivity = DefaultConnectivity(scope, provider, ConnectivityOptions())
        connectivity.start()
        runCurrent()
        scope.cancel()
        connectivity.start()
        runCurrent()
        assertFalse(connectivity.isMonitoring)
    }

    @Test
    fun ownerCancellationClearsMonitoringState() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val provider = object : ConnectivityProvider {
            override fun monitor() = flow {
                emit(Connectivity.Status.Connected(false))
                awaitCancellation()
            }
        }
        val connectivity = DefaultConnectivity(scope, provider, ConnectivityOptions())
        connectivity.start()
        runCurrent()
        assertTrue(connectivity.isMonitoring)
        scope.cancel()
        runCurrent()
        assertFalse(connectivity.isMonitoring)
    }

    @Test
    fun stopPreservesOwnerAndAllowsRestart() = runTest {
        val owner = SupervisorJob()
        val scope = CoroutineScope(owner + StandardTestDispatcher(testScheduler))
        var subscriptions = 0
        var activeSubscriptions = 0
        val provider = object : ConnectivityProvider {
            override fun monitor() = flow {
                subscriptions++
                activeSubscriptions++
                try {
                    emit(Connectivity.Status.Connected(false))
                    awaitCancellation()
                } finally {
                    activeSubscriptions--
                }
            }
        }
        val connectivity = DefaultConnectivity(scope, provider, ConnectivityOptions())
        try {
            val sibling = scope.launch { awaitCancellation() }
            connectivity.start()
            runCurrent()
            connectivity.stop()
            runCurrent()
            assertTrue(owner.isActive)
            assertTrue(sibling.isActive)
            assertFalse(connectivity.isMonitoring)
            assertEquals(0, activeSubscriptions)
            connectivity.start()
            runCurrent()
            assertEquals(2, subscriptions)
            assertEquals(1, activeSubscriptions)
            assertTrue(connectivity.isMonitoring)
            // The old collector completes after its replacement has been assigned.
            connectivity.start()
            runCurrent()
            assertEquals(3, subscriptions)
            assertEquals(1, activeSubscriptions)
            assertTrue(connectivity.isMonitoring)
        } finally {
            scope.cancel()
        }
    }
}
