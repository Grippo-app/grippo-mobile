package com.grippo.core.internal.operation

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

internal interface OperationManager {
    fun launch(
        dispatcher: CoroutineDispatcher,
        onChildError: suspend (Throwable) -> Unit,
        block: suspend CoroutineScope.() -> Unit
    ): Job
}