package com.grippo.services.firebase

public object FirebaseProvider {

    private var analytics: FirebaseAnalyticsProvider? = null

    public fun init(analytics: FirebaseAnalyticsProvider) {
        this.analytics = analytics
    }

    public fun logEvent(
        name: String,
        params: Map<String, String> = emptyMap()
    ) {
        analytics?.logEvent(name, params)
    }
}