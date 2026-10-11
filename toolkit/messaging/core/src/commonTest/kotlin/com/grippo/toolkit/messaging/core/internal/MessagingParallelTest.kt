package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.synchronization.PushTokenSyncStatus
import com.grippo.toolkit.messaging.synchronization.PushTokenSynchronizer
import com.grippo.toolkit.messaging.token.PushToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class MessagingParallelTest {
    @Test fun concurrentPlatformCallbacksDeliverLatestTokenOnOneWorker() = runBlocking {
        val received = mutableListOf<String>()
        val core = DefaultMessaging(object : PushTokenSynchronizer {
            override suspend fun syncToken(token: PushToken) { received += token.value }
        })
        try {
            List(4) { producer ->
                launch(Dispatchers.Default) {
                    repeat(50) { core.acceptToken(PushToken("token_${producer}_$it")) }
                }
            }.forEach { it.join() }
            core.acceptToken(PushToken("final"))
            withTimeout(5000) { core.tokenSyncStatus.first { it == PushTokenSyncStatus.Synced } }
            assertEquals("final", received.last())
            assertTrue(received.size in 1..201)
        } finally { core.close() }
    }
}
