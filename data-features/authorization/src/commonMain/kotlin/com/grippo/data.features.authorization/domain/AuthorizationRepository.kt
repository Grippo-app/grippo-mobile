package com.grippo.data.features.authorization.domain

import com.grippo.data.features.api.authorization.models.SetRegistration
import kotlinx.coroutines.flow.Flow

internal interface AuthorizationRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(registration: SetRegistration): Result<Unit>
    suspend fun logout()
    fun getToken(): Flow<String?>
}