package com.grippo.data.features.api.authorization

import com.grippo.data.features.api.user.UserFeature

public class LoginUseCase(
    private val authorizationFeature: AuthorizationFeature,
    private val userFeature: UserFeature
) {
    public suspend fun execute(email: String, password: String) {
        authorizationFeature.login(email, password).getOrThrow()
        userFeature.getUser().getOrThrow()
    }
}