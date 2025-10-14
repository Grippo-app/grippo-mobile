package com.grippo.core.foundation.internal.operation

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

internal interface OperationManager {
    fun launch(
        dispatcher: CoroutineDispatcher,
        onError: suspend (Throwable) -> Unit,
        block: suspend CoroutineScope.() -> Unit
    ): Job

    fun <T> whileActive(
        upstream: Flow<T>,
        activation: Flow<Boolean>,
    ): Flow<T>
}