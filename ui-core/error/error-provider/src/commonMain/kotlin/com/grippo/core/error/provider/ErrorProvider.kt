package com.grippo.core.error.provider

public interface ErrorProvider {
    public suspend fun provide(exception: Throwable, callback: () -> Unit)
}