package com.grippo.core.internal

import com.grippo.core.models.BaseResult
import com.grippo.core.models.Result
import com.grippo.core.models.ResultKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Single

@Factory
internal class ResultManager(
    private val resultEmitter: ResultEmitter,
    @InjectedParam val coroutineScope: CoroutineScope
) {
    private val activeSubscriptions = mutableListOf<Job>()

    fun <T : BaseResult> observeResult(
        key: ResultKey<T>,
        onResult: suspend (T) -> Unit
    ) {
        val job = resultEmitter.results
            .filter { (resultKey, _) -> resultKey == key.key }
            .onEach { (_, result) -> @Suppress("UNCHECKED_CAST") onResult(result as T) }
            .launchIn(coroutineScope)

        activeSubscriptions.add(job)
    }

    fun <T : Any> sendResult(key: ResultKey<Result<T>>, data: T) {
        resultEmitter.sendResult(key, data)
    }

    fun clear() {
        activeSubscriptions.forEach { it.cancel() }
        activeSubscriptions.clear()
    }
}

@Single
internal class ResultEmitter {

    private val resultChannel = Channel<Pair<String, BaseResult>>(capacity = Channel.BUFFERED)

    val results: Flow<Pair<String, BaseResult>> = resultChannel.receiveAsFlow()

    fun <T : Any> sendResult(key: ResultKey<Result<T>>, data: T) {
        resultChannel.trySend(key.key to Result(data))
    }

    fun clear() {
        resultChannel.close()
    }
}