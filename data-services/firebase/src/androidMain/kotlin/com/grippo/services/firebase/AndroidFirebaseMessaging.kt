package com.grippo.services.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.grippo.toolkit.messaging.state.PushTokenState
import com.grippo.toolkit.messaging.transport.PushTokenParser
import com.grippo.toolkit.messaging.transport.PushTokenProvider
import kotlinx.coroutines.tasks.await

public class AndroidFirebaseMessaging : PushTokenProvider {
    override suspend fun fetchToken(): PushTokenState {
        val value = FirebaseMessaging.getInstance().token.await()
        return PushTokenParser.parse(value)
    }
}
