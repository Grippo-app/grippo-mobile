package com.grippo.toolkit.crashreporting.transport

public object CrashExceptionType {
    public fun nameOf(throwable: Throwable): String =
        throwable::class.qualifiedName ?: throwable::class.simpleName ?: "Throwable"
}
