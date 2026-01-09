package com.grippo.services.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

public class AndroidFirebaseAnalytics(
    private val firebase: FirebaseAnalytics
) : FirebaseAnalyticsProvider {

    override fun logEvent(
        name: String,
        params: Map<String, String>
    ) {
        val bundle = Bundle().apply {
            params.forEach { (k, v) -> putString(k, v) }
        }
        firebase.logEvent(name, bundle)
    }
}