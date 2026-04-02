package com.grippo.services.firebase

public interface MessagingEventHandler {

    /** Called when Firebase rotates the FCM registration token. */
    public suspend fun onTokenRefresh(token: String)
}
