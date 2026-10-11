package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.Messaging
import com.grippo.toolkit.messaging.core.MessagingModule
import com.grippo.toolkit.messaging.lifecycle.MessagingBootstrap
import com.grippo.toolkit.messaging.synchronization.PushTokenSyncStatus
import com.grippo.toolkit.messaging.synchronization.PushTokenSynchronizer
import com.grippo.toolkit.messaging.token.PushToken
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
                single<PushTokenSynchronizer> {
                    get<Messaging>()
                    object : PushTokenSynchronizer {
                        override suspend fun syncToken(token: PushToken) { delivered.complete(token) }
                    }
                }
            })
        }
        val firstDelivered = CompletableDeferred<PushToken>()
        val secondDelivered = CompletableDeferred<PushToken>()
        val first = application(firstDelivered)
        val second = application(secondDelivered)
        try {
            first.koin.get<MessagingBootstrap>().onTokenChanged("first")
            second.koin.get<MessagingBootstrap>().onTokenChanged("second")
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
                single<PushTokenSynchronizer> {
                    get<Messaging>() // Mirrors the authorization repository's dependency on Messaging.
                    object : PushTokenSynchronizer { override suspend fun syncToken(token: PushToken) = Unit }
                }
            })
        }
        val messaging = application.koin.get<DefaultMessaging>()
        try {
            assertSame(messaging, application.koin.get<Messaging>())
            assertSame(messaging, application.koin.get<MessagingBootstrap>())
            application.koin.get<MessagingBootstrap>().onTokenChanged("fixture")
            withTimeout(5000) { messaging.tokenSyncStatus.first { it == PushTokenSyncStatus.Synced } }
        } finally {
            messaging.close()
            application.close()
        }
    }
}
