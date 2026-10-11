package com.grippo.core.foundation.internal.result

import com.grippo.core.foundation.models.BaseResult
import com.grippo.core.foundation.models.Result
import com.grippo.core.foundation.models.ResultKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.annotation.Single

@Single
internal class ResultEmitter {

    private val resultFlow = MutableSharedFlow<Pair<String, BaseResult>>(extraBufferCapacity = 64)

    val results: Flow<Pair<String, BaseResult>> = resultFlow.asSharedFlow()

    suspend fun <T : Any> sendResult(key: ResultKey<Result<T>>, data: T) {
        resultFlow.emit(key.key to Result(data))
    }
}
