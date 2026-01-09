package com.grippo.services.firebase

public interface FirebaseAnalyticsProvider {
    public fun logEvent(name: String, params: Map<String, String>)
}