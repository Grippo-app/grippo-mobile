package com.grippo.services.firebase

import com.google.firebase.crashlytics.FirebaseCrashlytics

public class AndroidFirebaseCrashlytics(
    private val core: FirebaseCrashlytics
) : FirebaseCrashlyticsProvider {

    override fun log(message: String) {
        core.log(message)
    }

    override fun recordException(throwable: Throwable, metadata: Map<String, String>) {
        metadata.forEach { (key, value) -> core.setCustomKey(key, value) }
        core.recordException(throwable)
    }
}