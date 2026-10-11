package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.PushToken
import com.grippo.toolkit.messaging.PushTokenHandler
import com.grippo.toolkit.messaging.TokenSyncStatus
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
        val core = DefaultMessaging(object : PushTokenHandler {
            override suspend fun onTokenRefresh(token: PushToken) { received += token.value }
        })
        try {
            List(4) { producer ->
                launch(Dispatchers.Default) {
                    repeat(50) { core.onTokenRefresh(PushToken("token_${producer}_$it")) }
                }
            }.forEach { it.join() }
            core.onTokenRefresh(PushToken("final"))
            withTimeout(5000) { core.syncStatus.first { it == TokenSyncStatus.Synced } }
            assertEquals("final", received.last())
            assertTrue(received.size in 1..201)
        } finally { core.close() }
    }
}
