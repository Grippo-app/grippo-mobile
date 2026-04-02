package com.grippo.data.features.authorization.domain

import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.services.firebase.MessagingEventHandler
import org.koin.core.annotation.Single

@Single(binds = [MessagingEventHandler::class])
internal class MessagingEventHandlerImpl(
    private val authorizationFeature: AuthorizationFeature,
) : MessagingEventHandler {

    override suspend fun onTokenRefresh(token: String) {
        authorizationFeature.updatePushToken(token)
    }
}
