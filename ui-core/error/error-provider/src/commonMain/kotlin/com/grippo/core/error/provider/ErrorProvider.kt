package com.grippo.core.error.provider

public interface ErrorProvider {
    public suspend fun retry(error: Throwable): ErrorDecision
    public suspend fun inform(error: Throwable)
}