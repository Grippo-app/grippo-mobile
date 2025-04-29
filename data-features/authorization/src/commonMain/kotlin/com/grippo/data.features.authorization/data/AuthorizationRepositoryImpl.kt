package com.grippo.data.features.authorization.data

import com.grippo.data.features.api.authorization.models.Registration
import com.grippo.data.features.authorization.domain.AuthorizationRepository
import com.grippo.database.dao.TokenDao
import com.grippo.database.entity.TokenEntity
import com.grippo.domain.mapper.toDto
import com.grippo.network.Api
import com.grippo.network.dto.AuthDto
import com.grippo.network.mapper.toEntityOrNull

internal class AuthorizationRepositoryImpl(
    private val api: Api,
    private val tokenDao: TokenDao
) : AuthorizationRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        val response = api.login(AuthDto(email, password))

        response.onSuccess { r ->
            val entity = r.toEntityOrNull() ?: return@onSuccess
            tokenDao.insert(entity)
        }

        return response.map { }
    }

    override suspend fun register(registration: Registration): Result<Unit> {
        val response = api.register(registration.toDto())

        response.onSuccess { r ->
            tokenDao.insert(TokenEntity(access = r.accessToken))
        }

        return response.map { }
    }
}