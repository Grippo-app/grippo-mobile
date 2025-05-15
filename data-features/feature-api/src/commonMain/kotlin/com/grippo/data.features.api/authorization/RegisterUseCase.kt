package com.grippo.data.features.api.authorization

import com.grippo.data.features.api.authorization.models.SetRegistration
import com.grippo.data.features.api.user.UserFeature

public class RegisterUseCase(
    private val authorizationFeature: AuthorizationFeature,
    private val userFeature: UserFeature
) {
    public suspend fun execute(registration: SetRegistration) {
        authorizationFeature.register(registration).getOrThrow()
        userFeature.getUser().getOrThrow()
    }
}