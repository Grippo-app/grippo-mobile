package com.grippo.data.features.user.data

import com.grippo.backend.GrippoApi
import com.grippo.data.features.api.user.models.User
import com.grippo.data.features.user.domain.UserRepository
import com.grippo.database.dao.UserActiveDao
import com.grippo.database.dao.UserDao
import com.grippo.database.domain.user.toDomain
import com.grippo.dto.entity.user.toEntityOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [UserRepository::class])
internal class UserRepositoryImpl(
    private val api: GrippoApi,
    private val userDao: UserDao,
    private val userActiveDao: UserActiveDao
) : UserRepository {

    override fun observeUser(): Flow<User?> {
        return userActiveDao.get()
            .flatMapLatest { activeId ->
                if (activeId == null) flowOf(null)
                else userDao.getById(activeId)
            }.map { it?.toDomain() }
    }

    override suspend fun getUser(): Result<Unit> {
        val response = api.getUser()

        response.onSuccess {
            val user = it.toEntityOrNull() ?: return@onSuccess
            userDao.insertOrUpdate(user)
        }

        return response.map { }
    }
}