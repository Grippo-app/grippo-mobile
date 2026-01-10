package com.grippo.services.firebase

public object FirebaseProvider {

    private var analytics: FirebaseAnalyticsProvider? = null

    public fun setup(analytics: FirebaseAnalyticsProvider) {
        this.analytics = analytics
    }

    public enum class Event(internal val key: String) {
        REGISTRATION_COMPLETED("registration_completed"),
        WORKOUT_STARTED("workout_started"),
        WORKOUT_COMPLETED("workout_completed"),
    }

    public fun logEvent(event: Event, params: Map<String, String> = emptyMap()) {
        analytics?.logEvent(event.key, params)
    }
}