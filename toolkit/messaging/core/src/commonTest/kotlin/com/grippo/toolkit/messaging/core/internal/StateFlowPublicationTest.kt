package com.grippo.toolkit.messaging.core.internal

import com.grippo.toolkit.messaging.TokenSyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class StateFlowPublicationTest {
    @Test fun aDelayedPendingPublicationCannotOverwriteACompletedSync() {
        val state = MutableStateFlow(TokenSyncStatus.Synced)
        var canonical = TokenSyncStatus.Pending
        var delayed = true
        lateinit var controlled: MutableStateFlow<TokenSyncStatus>
        controlled = object : MutableStateFlow<TokenSyncStatus> by state {
            override fun compareAndSet(expect: TokenSyncStatus, update: TokenSyncStatus): Boolean {
                if (delayed) {
                    delayed = false
                    // Another publisher completes while this publisher has already read Pending.
                    // StateFlow is already Synced, so that publication does not change the CAS value.
                    canonical = TokenSyncStatus.Synced
                    publishState(controlled) { canonical }
                }
                return state.compareAndSet(expect, update)
            }
        }
        publishState(controlled) { canonical }
        assertEquals(TokenSyncStatus.Synced, state.value)
    }
}
