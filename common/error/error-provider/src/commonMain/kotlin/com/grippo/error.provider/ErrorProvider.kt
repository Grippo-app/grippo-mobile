package com.grippo.error.provider

public interface ErrorProvider {
    public fun provide(exception: Throwable, callback: () -> Unit)
}