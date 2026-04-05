package com.grippo.services.firebase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public object MessagingEventBridge : KoinComponent {

    private val handler: MessagingEventHandler by inject()
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    public fun onTokenRefresh(token: String) {
        scope.launch { handler.onTokenRefresh(token) }
    }
}
