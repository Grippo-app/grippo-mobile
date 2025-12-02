package com.grippo.data.features.authorization.domain

import com.grippo.data.features.api.authorization.AuthorizationFeature
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single(binds = [AuthorizationFeature::class])
internal class AuthorizationFeatureImpl(
    private val repository: AuthorizationRepository
) : AuthorizationFeature {

    override suspend fun loginByEmail(email: String, password: String): Result<Unit> {
        return repository.loginByEmail(email, password)
    }

    override suspend fun loginByGoogle(token: String): Result<Unit> {
        return repository.loginByGoogle(token)
    }

    override suspend fun register(email: String, password: String): Result<Unit> {
        return repository.register(email, password)
    }

    override fun getToken(): Flow<String?> {
        return repository.getToken()
    }

    override suspend fun logout() {
        return repository.logout()
    }
}
