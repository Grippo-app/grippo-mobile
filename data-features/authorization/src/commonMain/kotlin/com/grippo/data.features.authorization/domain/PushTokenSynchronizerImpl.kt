package com.grippo.data.features.authorization.domain

import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.toolkit.messaging.synchronization.PushTokenSynchronizer
import com.grippo.toolkit.messaging.token.PushToken
import org.koin.core.annotation.Single

@Single(binds = [PushTokenSynchronizer::class])
internal class PushTokenSynchronizerImpl(
    private val authorizationFeature: AuthorizationFeature,
) : PushTokenSynchronizer {

    override suspend fun syncToken(token: PushToken) {
        authorizationFeature.updatePushToken(token.value).getOrThrow()
    }
}
