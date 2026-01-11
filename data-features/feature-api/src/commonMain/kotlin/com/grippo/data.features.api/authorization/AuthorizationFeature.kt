package com.grippo.data.features.api.authorization

import kotlinx.coroutines.flow.Flow

public interface AuthorizationFeature {

    public suspend fun login(email: String, password: String): Result<Unit>

    public suspend fun google(token: String): Result<Unit>

    public suspend fun register(email: String, password: String): Result<Unit>

    public suspend fun logout()

    public fun getToken(): Flow<String?>
}
