package com.grippo.services.firebase

public interface FirebaseMessagingProvider {

    public suspend fun getToken(): String?
}
