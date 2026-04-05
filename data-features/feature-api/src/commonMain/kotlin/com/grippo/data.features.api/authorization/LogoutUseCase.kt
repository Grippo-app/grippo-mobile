package com.grippo.data.features.api.authorization

public class LogoutUseCase(
    private val authorizationFeature: AuthorizationFeature,
) {
    public suspend fun execute() {
        authorizationFeature.deletePushToken()
        authorizationFeature.logout()
    }
}
