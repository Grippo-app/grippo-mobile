package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.state.PushTokenState
import com.grippo.toolkit.messaging.synchronization.PushTokenSynchronizer
import com.grippo.toolkit.messaging.token.PushToken
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MessagingConcurrencyTest {
    @Test fun reentrantObserverNeverRunsSynchronizerUnderAnOuterStateLock(): Unit = runBlocking {
        val observer = Executors.newSingleThreadExecutor()
        lateinit var core: DefaultMessaging
        var observedOutsideLock = false
        core = DefaultMessaging(object : PushTokenSynchronizer {
            override suspend fun syncToken(token: PushToken) {
                if (token.value != "second") return
                val observed = CountDownLatch(1)
                observer.submit { core.onTokenChanged("observer"); observed.countDown() }
                observedOutsideLock = observed.await(1, TimeUnit.SECONDS)
            }
        }, Dispatchers.Unconfined)
        val collector = launch(Dispatchers.Unconfined) {
            core.tokenState.collect { result ->
                if (result is PushTokenState.Available && result.token.value == "first") {
                    core.onTokenChanged("second")
                }
            }
        }
        try {
            core.onTokenChanged("first")
            assertTrue(observedOutsideLock, "Synchronizer ran under a reentrant producer state lock")
        } finally {
            collector.cancel()
            core.close()
            observer.shutdownNow()
            assertTrue(observer.awaitTermination(5, TimeUnit.SECONDS))
        }
    }
}
