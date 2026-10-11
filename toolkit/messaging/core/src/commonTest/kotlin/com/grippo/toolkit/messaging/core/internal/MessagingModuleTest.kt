package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.Messaging
import com.grippo.toolkit.messaging.MessagingBootstrap
import com.grippo.toolkit.messaging.PushToken
import com.grippo.toolkit.messaging.PushTokenHandler
import com.grippo.toolkit.messaging.TokenSyncStatus
import com.grippo.toolkit.messaging.core.MessagingModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.ksp.generated.module

class MessagingModuleTest {
    @Test fun callbackUsesItsOwnContainerWithoutAGlobalKoinContext(): Unit = runBlocking {
        fun application(delivered: CompletableDeferred<PushToken>) = koinApplication {
            modules(MessagingModule().module, module {
                single<PushTokenHandler> {
                    get<Messaging>()
                    object : PushTokenHandler {
                        override suspend fun onTokenRefresh(token: PushToken) { delivered.complete(token) }
                    }
                }
            })
        }
        val firstDelivered = CompletableDeferred<PushToken>()
        val secondDelivered = CompletableDeferred<PushToken>()
        val first = application(firstDelivered)
        val second = application(secondDelivered)
        try {
            first.koin.get<MessagingBootstrap>().onRegistrationToken("first")
            second.koin.get<MessagingBootstrap>().onRegistrationToken("second")
            withTimeout(5000) {
                assertEquals(PushToken("first"), firstDelivered.await())
                assertEquals(PushToken("second"), secondDelivered.await())
            }
        } finally {
            first.koin.get<DefaultMessaging>().close()
            second.koin.get<DefaultMessaging>().close()
            first.close()
            second.close()
        }
    }

    @Test fun compositionResolvesSingletonsAndDeferredCallbackWithoutCycle(): Unit = runBlocking {
        val application = koinApplication {
            modules(MessagingModule().module, module {
                single<PushTokenHandler> {
                    get<Messaging>() // Mirrors the authorization repository's dependency on Messaging.
                    object : PushTokenHandler { override suspend fun onTokenRefresh(token: PushToken) = Unit }
                }
            })
        }
        val messaging = application.koin.get<DefaultMessaging>()
        try {
            assertSame(messaging, application.koin.get<Messaging>())
            assertSame(messaging, application.koin.get<MessagingBootstrap>())
            application.koin.get<MessagingBootstrap>().onRegistrationToken("fixture")
            withTimeout(5000) { messaging.syncStatus.first { it == TokenSyncStatus.Synced } }
        } finally {
            messaging.close()
            application.close()
        }
    }
}
