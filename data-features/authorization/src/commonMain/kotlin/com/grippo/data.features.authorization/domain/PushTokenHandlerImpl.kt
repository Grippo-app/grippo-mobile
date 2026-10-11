package com.grippo.data.features.authorization.domain

import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.toolkit.messaging.PushToken
import com.grippo.toolkit.messaging.PushTokenHandler
import org.koin.core.annotation.Single

@Single(binds = [PushTokenHandler::class])
internal class PushTokenHandlerImpl(
    private val authorizationFeature: AuthorizationFeature,
) : PushTokenHandler {

    override suspend fun onTokenRefresh(token: PushToken) {
        authorizationFeature.updatePushToken(token.value).getOrThrow()
    }
}
