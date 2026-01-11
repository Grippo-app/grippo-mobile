package com.grippo.data.features.authorization.domain

import kotlinx.coroutines.flow.Flow

internal interface AuthorizationRepository {

    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun google(token: String): Result<Unit>

    suspend fun register(email: String, password: String): Result<Unit>

    suspend fun logout()

    fun getToken(): Flow<String?>
}
