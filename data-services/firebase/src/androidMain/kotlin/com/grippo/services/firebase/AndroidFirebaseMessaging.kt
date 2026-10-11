package com.grippo.services.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.grippo.toolkit.messaging.MessagingSink
import com.grippo.toolkit.messaging.PushTokenDecoder
import com.grippo.toolkit.messaging.PushTokenResult
import kotlinx.coroutines.tasks.await

public class AndroidFirebaseMessaging : MessagingSink {
    override suspend fun getToken(): PushTokenResult {
        val value = FirebaseMessaging.getInstance().token.await()
        return PushTokenDecoder.decode(value)
    }
}
