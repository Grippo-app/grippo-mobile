package com.grippo.services.firebase

public interface FirebaseCrashlyticsProvider {

    public fun log(message: String)

    public fun recordException(throwable: Throwable, metadata: Map<String, String>)
}