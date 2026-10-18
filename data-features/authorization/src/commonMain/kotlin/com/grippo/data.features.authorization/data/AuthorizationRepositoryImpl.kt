package com.grippo.data.features.authorization.data

import com.grippo.core.error.provider.AppError
import com.grippo.data.features.authorization.domain.AuthorizationRepository
import com.grippo.dto.entity.user.toEntityOrNull
import com.grippo.services.backend.GrippoApi
import com.grippo.services.backend.dto.auth.AppleBody
import com.grippo.services.backend.dto.auth.EmailAuthBody
import com.grippo.services.backend.dto.auth.GoogleBody
import com.grippo.services.backend.dto.auth.RegisterBody
import com.grippo.services.backend.dto.auth.TokenResponse
import com.grippo.services.backend.dto.push.PushTokenBody
import com.grippo.services.database.dao.TokenDao
import com.grippo.services.database.dao.UserActiveDao
import com.grippo.services.database.entity.UserActiveEntity
import com.grippo.toolkit.messaging.Messaging
import com.grippo.toolkit.messaging.state.PushTokenState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [AuthorizationRepository::class])
internal class AuthorizationRepositoryImpl(
    private val api: GrippoApi,
    private val messaging: Messaging,
    private val tokenDao: TokenDao,
    private val userActiveDao: UserActiveDao,
) : AuthorizationRepository {

    override suspend fun login(email: String, password: String): Unit {
        val response = api.login(EmailAuthBody(email = email, password = password))

        saveSession(response)
    }

    override suspend fun google(token: String): Unit {
        val response = api.google(GoogleBody(idToken = token))

        saveSession(response)
    }

    override suspend fun apple(code: String): Unit {
        val response = api.apple(AppleBody(code = code))

        saveSession(response)
    }

    override suspend fun register(email: String, password: String): Unit {
        val response = api.register(RegisterBody(email = email, password = password))

        saveSession(response)
    }

    override fun getToken(): Flow<String?> {
        return userActiveDao.get()
            .flatMapLatest { activeId ->
                if (activeId == null) flowOf(null)
                else tokenDao.getById(activeId)
            }.map {
                it?.access
            }
    }

    override suspend fun logout() {
        val activeId = userActiveDao.get().firstOrNull() ?: return
        tokenDao.delete(activeId)
    }

    override suspend fun updatePushToken(token: String): Unit {
        return api.sendPushToken(PushTokenBody(token = token))
    }

    override suspend fun getPushToken(): String? {
        return when (val result = messaging.fetchToken()) {
            is PushTokenState.Available -> result.token.value
            is PushTokenState.Unavailable -> null
            is PushTokenState.Failed -> throw IllegalStateException("Push token retrieval failed: ${result.reason}")
        }
    }

    override suspend fun deletePushToken(): Unit {
        return api.deletePushToken()
    }

    private suspend fun saveSession(response: TokenResponse) {
        val entity = response.toEntityOrNull() ?: throw AppError.Network.IncompleteResponse()
        if (entity.id.isBlank() || entity.access.isNullOrBlank() || entity.refresh.isNullOrBlank()) {
            throw AppError.Network.IncompleteResponse()
        }
        tokenDao.insertOrUpdate(entity)
        userActiveDao.insertOrReplace(UserActiveEntity(userId = entity.id))
    }
}
