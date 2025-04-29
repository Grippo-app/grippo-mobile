package com.grippo.data.features.api.authorization

import com.grippo.data.features.api.authorization.models.Registration

public interface AuthorizationFeature {
    public suspend fun login(email: String, password: String): Result<Unit>
    public suspend fun register(registration: Registration): Result<Unit>
}