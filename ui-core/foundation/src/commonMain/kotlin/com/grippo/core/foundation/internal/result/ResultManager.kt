package com.grippo.core.foundation.internal.result

import com.grippo.core.foundation.models.BaseResult
import com.grippo.core.foundation.models.Result
import com.grippo.core.foundation.models.ResultKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

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
        // Subscribe before returning so an immediate result cannot race registration.
        val job = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            resultEmitter.results
                .filter { (resultKey, _) -> resultKey == key.key }
                .collect { (_, result) -> @Suppress("UNCHECKED_CAST") onResult(result as T) }
        }

        activeSubscriptions.add(job)
    }

    fun <T : Any> sendResult(key: ResultKey<Result<T>>, data: T) {
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            ensureActive()
            resultEmitter.sendResult(key, data)
        }
    }

    fun clear() {
        activeSubscriptions.forEach { it.cancel() }
        activeSubscriptions.clear()
    }
}
