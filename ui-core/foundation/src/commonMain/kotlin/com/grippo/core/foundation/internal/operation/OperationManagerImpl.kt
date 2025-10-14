package com.grippo.core.foundation.internal.operation

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

@Factory(binds = [OperationManager::class])
internal class OperationManagerImpl(
    @InjectedParam val coroutineScope: CoroutineScope
) : OperationManager {

    override fun launch(
        dispatcher: CoroutineDispatcher,
        onError: suspend (Throwable) -> Unit,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val handler = CoroutineExceptionHandler { _, t ->
            if (t !is CancellationException) coroutineScope.launch { onError(t) }
        }
        return coroutineScope.launch(
            dispatcher + handler + SupervisorJob(coroutineScope.coroutineContext[Job])
        ) {
            supervisorScope { block() }
        }
    }

    override fun <T> whileActive(
        upstream: Flow<T>,
        activation: Flow<Boolean>,
    ): Flow<T> = activation
        .flatMapLatest { active ->
            if (active) flowOf(true) else flow {
                delay(1.seconds)
                emit(false)
            }
        }
        .distinctUntilChanged()
        .flatMapLatest { active -> if (active) upstream else emptyFlow() }
}