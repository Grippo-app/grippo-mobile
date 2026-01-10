package com.grippo.services.firebase

public object FirebaseProvider {

    private var analytics: FirebaseAnalyticsProvider? = null
    private var crashlytics: FirebaseCrashlyticsProvider? = null

    public fun setup(
        analytics: FirebaseAnalyticsProvider,
        crashlytics: FirebaseCrashlyticsProvider,
    ) {
        this.analytics = analytics
        this.crashlytics = crashlytics
    }

    public enum class Event(internal val key: String) {
        REGISTRATION_COMPLETED("registration_completed"),
        WORKOUT_STARTED("workout_started"),
        WORKOUT_COMPLETED("workout_completed"),
    }

    public fun logEvent(event: Event, params: Map<String, String> = emptyMap()) {
        analytics?.logEvent(event.key, params)
    }

    public fun recordException(throwable: Throwable, metadata: Map<String, String> = emptyMap()) {
        crashlytics?.recordException(throwable, metadata)
    }
}