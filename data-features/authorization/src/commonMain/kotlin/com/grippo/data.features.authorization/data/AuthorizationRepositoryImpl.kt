package com.grippo.data.features.authorization.data

import com.grippo.data.features.api.authorization.models.SetRegistration
import com.grippo.data.features.authorization.domain.AuthorizationRepository
import com.grippo.database.dao.TokenDao
import com.grippo.database.dao.UserActiveDao
import com.grippo.database.entity.UserActiveEntity
import com.grippo.domain.network.user.toBody
import com.grippo.network.Api
import com.grippo.network.database.user.toEntityOrNull
import com.grippo.network.dto.auth.AuthBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [AuthorizationRepository::class])
internal class AuthorizationRepositoryImpl(
    private val api: Api,
    private val tokenDao: TokenDao,
    private val userActiveDao: UserActiveDao,
) : AuthorizationRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        val response = api.login(AuthBody(email, password))

        response.onSuccess { r ->
            val entity = r.toEntityOrNull() ?: return@onSuccess
            tokenDao.insertOrUpdate(entity)
            userActiveDao.insertOrReplace(UserActiveEntity(userId = entity.id))
        }

        return response.map { }
    }

    override suspend fun register(registration: SetRegistration): Result<Unit> {
        val response = api.register(registration.toBody())

        response.onSuccess { r ->
            val entity = r.toEntityOrNull() ?: return@onSuccess
            tokenDao.insertOrUpdate(entity)
            userActiveDao.insertOrReplace(UserActiveEntity(userId = entity.id))
        }

        return response.map { }
    }

    override fun getToken(): Flow<String?> {
        return userActiveDao.get()
            .flatMapLatest { activeId ->
                if (activeId == null) flowOf(null)
                else tokenDao.getById(activeId)
            }.map { it?.access }
    }

    override suspend fun logout() {
        val activeId = userActiveDao.get().firstOrNull() ?: return
        tokenDao.delete(activeId)
    }
}