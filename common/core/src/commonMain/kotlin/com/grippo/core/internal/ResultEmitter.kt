package com.grippo.core.internal

import com.grippo.core.models.BaseResult
import com.grippo.core.models.Result
import com.grippo.core.models.ResultKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.core.annotation.Single

internal interface ResultSender {
    fun <T : Any> sendResult(key: ResultKey<Result<T>>, data: T)
}

@Single(binds = [ResultSender::class])
internal class ResultEmitter : ResultSender {

    private val resultChannel = Channel<Pair<String, BaseResult>>(capacity = Channel.BUFFERED)

    val results: Flow<Pair<String, BaseResult>> = resultChannel.receiveAsFlow()

    override fun <T : Any> sendResult(key: ResultKey<Result<T>>, data: T) {
        resultChannel.trySend(key.key to Result(data))
    }

    fun clear() {
        resultChannel.close()
    }
}
