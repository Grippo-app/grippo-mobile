package com.grippo.data.features.user.data

import com.grippo.data.features.api.user.models.User
import com.grippo.data.features.user.domain.UserRepository
import com.grippo.database.dao.UserDao
import com.grippo.database.mapper.toDomain
import com.grippo.network.Api
import com.grippo.network.mapper.toEntityOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class UserRepositoryImpl(
    private val api: Api,
    private val userDao: UserDao,
) : UserRepository {

    override fun observeUser(): Flow<User?> {
        return userDao.get()
            .map { it?.toDomain() }
    }

    override suspend fun getUser(): Result<Unit> {
        val response = api.getUser()

        response.onSuccess {
            val user = it.toEntityOrNull() ?: return@onSuccess

            userDao.insertOrUpdateUser(user)
        }

        return response.map { }
    }
}