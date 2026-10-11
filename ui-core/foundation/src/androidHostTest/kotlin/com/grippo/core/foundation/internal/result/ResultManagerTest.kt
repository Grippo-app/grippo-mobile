package com.grippo.core.foundation.internal.result

import com.grippo.core.foundation.models.Result
import com.grippo.core.foundation.models.ResultKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ResultManagerTest {
    private val firstKey = ResultKey<Result<Int>>("first")
    private val secondKey = ResultKey<Result<Int>>("second")

    @Test fun differentKeysDoNotStealEachOthersResults() = runTest {
        val emitter = ResultEmitter()
        val first = ResultManager(emitter, backgroundScope)
        val second = ResultManager(emitter, backgroundScope)
        val firstResults = mutableListOf<Int>()
        val secondResults = mutableListOf<Int>()
        first.observeResult(firstKey) { firstResults += it.data }
        second.observeResult(secondKey) { secondResults += it.data }
        runCurrent()

        repeat(10) { second.sendResult(secondKey, it) }
        repeat(10) { first.sendResult(firstKey, it) }
        runCurrent()

        assertEquals((0..9).toList(), firstResults)
        assertEquals((0..9).toList(), secondResults)
    }

    @Test fun everyObserverOfTheSameKeyReceivesTheResult() = runTest {
        val emitter = ResultEmitter()
        val first = ResultManager(emitter, backgroundScope)
        val second = ResultManager(emitter, backgroundScope)
        val firstResults = mutableListOf<Int>()
        val secondResults = mutableListOf<Int>()
        first.observeResult(firstKey) { firstResults += it.data }
        second.observeResult(firstKey) { secondResults += it.data }
        runCurrent()
        first.sendResult(firstKey, 42)
        runCurrent()
        assertEquals(listOf(42), firstResults)
        assertEquals(listOf(42), secondResults)
    }

    @Test fun registrationIsReadyBeforeObserveResultReturns() = runTest {
        val manager = ResultManager(ResultEmitter(), backgroundScope)
        val received = mutableListOf<Int>()
        manager.observeResult(firstKey) { received += it.data }
        manager.sendResult(firstKey, 42)
        runCurrent()
        assertEquals(listOf(42), received)
    }

    @Test fun clearingOneManagerDoesNotStopOtherObservers() = runTest {
        val emitter = ResultEmitter()
        val first = ResultManager(emitter, backgroundScope)
        val second = ResultManager(emitter, backgroundScope)
        val firstResults = mutableListOf<Int>()
        val secondResults = mutableListOf<Int>()
        first.observeResult(firstKey) { firstResults += it.data }
        second.observeResult(firstKey) { secondResults += it.data }
        runCurrent()
        first.clear()
        second.sendResult(firstKey, 42)
        runCurrent()
        assertEquals(emptyList(), firstResults)
        assertEquals(listOf(42), secondResults)
    }

    @Test fun slowObserverReceivesBurstWithoutLosingResults() = runTest {
        val manager = ResultManager(ResultEmitter(), backgroundScope)
        val gate = CompletableDeferred<Unit>()
        val received = mutableListOf<Int>()
        manager.observeResult(firstKey) {
            gate.await()
            received += it.data
        }
        runCurrent()
        repeat(128) { manager.sendResult(firstKey, it) }
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals((0..127).toList(), received)
    }

    @Test fun newlyOpenedObserverDoesNotReceiveOldResults() = runTest {
        val manager = ResultManager(ResultEmitter(), backgroundScope)
        manager.sendResult(firstKey, 1)
        runCurrent()
        val received = mutableListOf<Int>()
        manager.observeResult(firstKey) { received += it.data }
        manager.sendResult(firstKey, 2)
        runCurrent()
        assertEquals(listOf(2), received)
    }

    @Test fun cancelledSenderDoesNotEmitLateResults() = runTest {
        val emitter = ResultEmitter()
        val observer = ResultManager(emitter, backgroundScope)
        val received = mutableListOf<Int>()
        observer.observeResult(firstKey) { received += it.data }
        val senderJob = Job(backgroundScope.coroutineContext[Job])
        val sender = ResultManager(emitter, CoroutineScope(backgroundScope.coroutineContext + senderJob))

        senderJob.cancel()
        sender.sendResult(firstKey, 42)
        runCurrent()

        assertEquals(emptyList(), received)
        observer.sendResult(firstKey, 43)
        runCurrent()
        assertEquals(listOf(43), received)
    }

    @Test fun closingSenderCancelsWaitingSendWithoutRetractingBufferedResults() = runTest {
        val emitter = ResultEmitter()
        val observer = ResultManager(emitter, backgroundScope)
        val gate = CompletableDeferred<Unit>()
        val received = mutableListOf<Int>()
        observer.observeResult(firstKey) {
            gate.await()
            received += it.data
        }
        val senderJob = Job(backgroundScope.coroutineContext[Job])
        val sender = ResultManager(emitter, CoroutineScope(backgroundScope.coroutineContext + senderJob))

        sender.sendResult(firstKey, 0)
        runCurrent()
        repeat(64) { sender.sendResult(firstKey, it + 1) }
        sender.sendResult(firstKey, 999)
        runCurrent()
        assertEquals(1, senderJob.children.count())

        senderJob.cancel()
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals((0..64).toList(), received)

        observer.sendResult(firstKey, 1000)
        runCurrent()
        assertEquals((0..64).toList() + 1000, received)
    }
}
