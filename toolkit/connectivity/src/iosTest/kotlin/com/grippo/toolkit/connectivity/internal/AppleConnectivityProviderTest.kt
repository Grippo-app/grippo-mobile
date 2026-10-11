package com.grippo.toolkit.connectivity.internal

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test

class AppleConnectivityProviderTest {
    @Test
    fun theSameFlowCanBeCollectedAgainAfterItsMonitorIsCancelled() = runBlocking {
        val flow = AppleConnectivityProvider().monitor()
        repeat(3) {
            // first() cancels its collection. Reusing a cancelled native monitor hangs here.
            withTimeout(5_000) { flow.first() }
        }
    }
}
