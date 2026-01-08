package com.grippo.data.features.authorization.data

import com.grippo.data.features.authorization.domain.AuthorizationRepository
import com.grippo.dto.entity.user.toEntityOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [AuthorizationRepository::class])
internal class AuthorizationRepositoryImpl(
    private val api: com.grippo.services.backend.GrippoApi,
    private val tokenDao: com.grippo.services.database.dao.TokenDao,
    private val userActiveDao: com.grippo.services.database.dao.UserActiveDao,
) : AuthorizationRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        val response = api.login(
            com.grippo.services.backend.dto.auth.EmailAuthBody(
                email,
                password
            )
        )

        response.onSuccess { r ->
            val entity = r.toEntityOrNull() ?: return@onSuccess
            tokenDao.insertOrUpdate(entity)
            userActiveDao.insertOrReplace(
                _root_ide_package_.com.grippo.services.database.entity.UserActiveEntity(
                    userId = entity.id
                )
            )
        }

        return response.map { }
    }

    override suspend fun google(token: String): Result<Unit> {
        val response = api.google(com.grippo.services.backend.dto.auth.GoogleBody(idToken = token))

        response.onSuccess { r ->
            val entity = r.toEntityOrNull() ?: return@onSuccess
            tokenDao.insertOrUpdate(entity)
            userActiveDao.insertOrReplace(
                _root_ide_package_.com.grippo.services.database.entity.UserActiveEntity(
                    userId = entity.id
                )
            )
        }

        return response.map { }
    }

    override suspend fun register(email: String, password: String): Result<Unit> {
        val response = api.register(
            com.grippo.services.backend.dto.auth.RegisterBody(
                email = email,
                password = password
            )
        )

        response.onSuccess { r ->
            val entity = r.toEntityOrNull() ?: return@onSuccess
            tokenDao.insertOrUpdate(entity)
            userActiveDao.insertOrReplace(
                _root_ide_package_.com.grippo.services.database.entity.UserActiveEntity(
                    userId = entity.id
                )
            )
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
