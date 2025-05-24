package com.grippo.data.features.authorization.data

import com.grippo.data.features.api.authorization.models.SetRegistration
import com.grippo.data.features.authorization.domain.AuthorizationRepository
import com.grippo.database.dao.TokenDao
import com.grippo.domain.mapper.toDto
import com.grippo.network.Api
import com.grippo.network.dto.AuthBody
import com.grippo.network.mapper.toEntityOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class AuthorizationRepositoryImpl(
    private val api: Api,
    private val tokenDao: TokenDao,
) : AuthorizationRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        val response = api.login(AuthBody(email, password))

        response.onSuccess { r ->
            val entity = r.toEntityOrNull() ?: return@onSuccess
            tokenDao.insert(entity)
        }

        return response.map { }
    }

    override suspend fun register(registration: SetRegistration): Result<Unit> {
        val response = api.register(registration.toDto())

        response.onSuccess { r ->
            val entity = r.toEntityOrNull() ?: return@onSuccess
            tokenDao.insert(entity)
        }

        return response.map { }
    }

    override fun getToken(): Flow<String?> {
        return tokenDao.get().map { it?.access }
    }

    override suspend fun logout() {

    }
}