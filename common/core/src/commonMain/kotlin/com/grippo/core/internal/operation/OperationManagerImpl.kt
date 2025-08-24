package com.grippo.core.internal.operation

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import kotlin.coroutines.cancellation.CancellationException

@Factory
internal class OperationManagerImpl(
    @InjectedParam val coroutineScope: CoroutineScope
) : OperationManager {

    override fun launch(
        dispatcher: CoroutineDispatcher,
        onChildError: suspend (Throwable) -> Unit,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val handler = CoroutineExceptionHandler { _, t ->
            if (t !is CancellationException) coroutineScope.launch { onChildError(t) }
        }
        return coroutineScope.launch(dispatcher + handler + SupervisorJob(coroutineScope.coroutineContext[Job])) {
            supervisorScope { block() }
        }
    }
}