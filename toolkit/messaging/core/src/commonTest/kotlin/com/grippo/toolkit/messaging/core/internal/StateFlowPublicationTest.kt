package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.synchronization.PushTokenSyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow

class StateFlowPublicationTest {
    @Test fun aDelayedPendingPublicationCannotOverwriteACompletedSync() {
        val state = MutableStateFlow(PushTokenSyncStatus.Synced)
        var canonical = PushTokenSyncStatus.Pending
        var delayed = true
        lateinit var controlled: MutableStateFlow<PushTokenSyncStatus>
        controlled = object : MutableStateFlow<PushTokenSyncStatus> by state {
            override fun compareAndSet(expect: PushTokenSyncStatus, update: PushTokenSyncStatus): Boolean {
                if (delayed) {
                    delayed = false
                    // Another publisher completes while this publisher has already read Pending.
                    // StateFlow is already Synced, so that publication does not change the CAS value.
                    canonical = PushTokenSyncStatus.Synced
                    publishState(controlled) { canonical }
                }
                return state.compareAndSet(expect, update)
            }
        }
        publishState(controlled) { canonical }
        assertEquals(PushTokenSyncStatus.Synced, state.value)
    }
}
