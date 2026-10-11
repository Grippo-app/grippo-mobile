package com.grippo.core.foundation.internal.operation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OperationManagerTest {
    @Test
    fun completedOperationsLeaveNoActiveChildren() = runTest {
        val owner = SupervisorJob()
        val scope = CoroutineScope(owner + StandardTestDispatcher(testScheduler))
        val manager = OperationManagerImpl(scope)
        try {
            repeat(100) {
                manager.launch(StandardTestDispatcher(testScheduler), { throw it }) {}.join()
            }
            assertEquals(0, owner.children.count())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun childFailureIsReportedWithoutCancellingAnotherOperation() = runTest {
        val owner = SupervisorJob()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(owner + dispatcher)
        val manager = OperationManagerImpl(scope)
        val failures = mutableListOf<Throwable>()
        val failure = IllegalStateException("child failed")
        try {
            val sibling = manager.launch(dispatcher, { failures.add(it) }) { awaitCancellation() }
            manager.launch(dispatcher, { failures.add(it) }) { launch { throw failure } }
            runCurrent()
            assertEquals(listOf<Throwable>(failure), failures)
            assertTrue(sibling.isActive)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun ownerCancellationCancelsWorkWithoutReportingAnError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val owner = SupervisorJob()
        val scope = CoroutineScope(owner + dispatcher)
        var reported = false
        val job = OperationManagerImpl(scope).launch(dispatcher, { reported = true }) {
            awaitCancellation()
        }
        runCurrent()
        scope.cancel()
        runCurrent()
        assertTrue(job.isCancelled)
        assertFalse(reported)
    }
}
