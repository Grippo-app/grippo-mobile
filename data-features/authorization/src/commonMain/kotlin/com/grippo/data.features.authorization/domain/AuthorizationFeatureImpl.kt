package com.grippo.data.features.authorization.domain

import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.authorization.models.SetRegistration
import kotlinx.coroutines.flow.Flow

internal class AuthorizationFeatureImpl(
    private val repository: AuthorizationRepository
) : AuthorizationFeature {

    override suspend fun login(email: String, password: String): Result<Unit> {
        return repository.login(email, password)
    }

    override suspend fun register(registration: SetRegistration): Result<Unit> {
        return repository.register(registration)
    }

    override fun getToken(): Flow<String?> {
        return repository.getToken()
    }

    override suspend fun logout() {
        return repository.logout()
    }
}