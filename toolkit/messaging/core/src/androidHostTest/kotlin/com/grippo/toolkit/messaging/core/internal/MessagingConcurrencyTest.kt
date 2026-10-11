package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.PushToken
import com.grippo.toolkit.messaging.PushTokenHandler
import com.grippo.toolkit.messaging.PushTokenResult
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MessagingConcurrencyTest {
    @Test fun reentrantObserverNeverRunsHandlerUnderAnOuterStateLock(): Unit = runBlocking {
        val observer = Executors.newSingleThreadExecutor()
        lateinit var core: DefaultMessaging
        var observedOutsideLock = false
        core = DefaultMessaging(object : PushTokenHandler {
            override suspend fun onTokenRefresh(token: PushToken) {
                if (token.value != "second") return
                val observed = CountDownLatch(1)
                observer.submit { core.onRegistrationToken("observer"); observed.countDown() }
                observedOutsideLock = observed.await(1, TimeUnit.SECONDS)
            }
        }, Dispatchers.Unconfined)
        val collector = launch(Dispatchers.Unconfined) {
            core.token.collect { result ->
                if (result is PushTokenResult.Available && result.token.value == "first") {
                    core.onRegistrationToken("second")
                }
            }
        }
        try {
            core.onRegistrationToken("first")
            assertTrue(observedOutsideLock, "Handler ran under a reentrant producer state lock")
        } finally {
            collector.cancel()
            core.close()
            observer.shutdownNow()
            assertTrue(observer.awaitTermination(5, TimeUnit.SECONDS))
        }
    }
}
