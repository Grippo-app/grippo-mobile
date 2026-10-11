package com.grippo.services.firebase

public object FirebaseProvider {

    private var crashlytics: FirebaseCrashlyticsProvider? = null
    private var messaging: FirebaseMessagingProvider? = null

    public fun setup(
        crashlytics: FirebaseCrashlyticsProvider,
        messaging: FirebaseMessagingProvider? = null,
    ) {
        this.crashlytics = crashlytics
        this.messaging = messaging
    }

    public suspend fun getMessagingToken(): String? {
        return messaging?.getToken()
    }

    public fun recordException(throwable: Throwable, metadata: Map<String, String> = emptyMap()) {
        crashlytics?.recordException(throwable, metadata)
    }

    // Swift may only see KotlinThrowable for exception classes not exported to Objective-C.
    public fun exceptionTypeName(throwable: Throwable): String =
        throwable::class.qualifiedName ?: throwable::class.simpleName ?: "Throwable"
}
