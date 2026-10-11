package com.grippo.core.foundation.models

import kotlinx.serialization.Serializable

public interface BaseResult

public data class Result<T : Any>(val data: T) : BaseResult

@Serializable(with = ResultKeySerializer::class)
public data class ResultKey<T : BaseResult>(val key: String) {
    override fun toString(): String = key
}

public object ResultKeys {
    public fun <T : BaseResult> create(key: String): ResultKey<T> = ResultKey(key)
}
