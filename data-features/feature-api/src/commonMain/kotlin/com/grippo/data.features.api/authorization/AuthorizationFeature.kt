package com.grippo.data.features.api.authorization

import com.grippo.data.features.api.authorization.models.SetRegistration
import kotlinx.coroutines.flow.Flow

public interface AuthorizationFeature {
    public suspend fun login(email: String, password: String): Result<Unit>
    public suspend fun register(registration: SetRegistration): Result<Unit>
    public suspend fun logout()
    public fun getToken(): Flow<String?>
}