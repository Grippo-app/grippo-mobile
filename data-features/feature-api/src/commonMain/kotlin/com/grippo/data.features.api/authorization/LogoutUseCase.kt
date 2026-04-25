package com.grippo.data.features.api.authorization

import com.grippo.data.features.api.local.settings.LocalSettingsFeature

public class LogoutUseCase(
    private val authorizationFeature: AuthorizationFeature,
    private val localSettingsFeature: LocalSettingsFeature
) {
    public suspend fun execute() {
        authorizationFeature.deletePushToken()
        localSettingsFeature.clear()
        authorizationFeature.logout()
    }
}
