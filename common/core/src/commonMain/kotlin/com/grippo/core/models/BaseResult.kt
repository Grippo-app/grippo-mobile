package com.grippo.core.models

public interface BaseResult

public data class Result<T : Any>(val data: T) : BaseResult

public data class ResultKey<T : BaseResult>(val key: String) {
    override fun toString(): String = key
}

public object ResultKeys {
    public fun <T : BaseResult> create(key: String): ResultKey<T> = ResultKey(key)
}
