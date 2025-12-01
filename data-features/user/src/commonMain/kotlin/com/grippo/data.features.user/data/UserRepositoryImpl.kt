package com.grippo.data.features.user.data

import com.grippo.backend.GrippoApi
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.data.features.api.user.models.User
import com.grippo.data.features.user.domain.UserRepository
import com.grippo.database.dao.UserActiveDao
import com.grippo.database.dao.UserDao
import com.grippo.domain.dto.user.toBody
import com.grippo.dto.entity.user.toEntityOrNull
import com.grippo.entity.domain.user.toDomain
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

    override suspend fun getUser(): Result<Boolean> {
        val response = api.getUser()

        return response.map { dto ->
            val user = dto.toEntityOrNull() ?: return@map false
            userDao.insertOrUpdate(user)
            true
        }
    }

    override suspend fun createProfile(profile: CreateUserProfile): Result<Boolean> {
        val response = api.createProfile(profile.toBody())

        return response.map { dto ->
            val user = dto.toEntityOrNull() ?: return@map false
            userDao.insertOrUpdate(user)
            true
        }
    }
}
