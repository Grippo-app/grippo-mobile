package com.grippo.services.firebase

public interface FirebaseMessagingProvider {

    /** Returns the current FCM registration token, or null if unavailable. */
    public suspend fun getToken(): String?
}
