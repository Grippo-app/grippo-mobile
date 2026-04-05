package com.grippo.services.firebase

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

public class AndroidFirebaseMessaging : FirebaseMessagingProvider {

    override suspend fun getToken(): String? {
        return runCatching {
            FirebaseMessaging.getInstance().token.await()
        }.getOrNull()
    }
}
