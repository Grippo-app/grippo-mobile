package com.grippo.data.features.user.data

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.data.features.api.user.models.User
import com.grippo.data.features.user.domain.UserRepository
import com.grippo.domain.dto.user.toBody
import com.grippo.dto.entity.user.toEntityOrNull
import com.grippo.entity.domain.user.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [UserRepository::class])
internal class UserRepositoryImpl(
    private val api: com.grippo.services.backend.GrippoApi,
    private val userDao: com.grippo.services.database.dao.UserDao,
    private val tokenDao: com.grippo.services.database.dao.TokenDao,
    private val userActiveDao: com.grippo.services.database.dao.UserActiveDao
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

    override suspend fun deleteProfile(): Result<Unit> {
        val response = api.deleteUser()

        return response.map {
            val activeId = userActiveDao.get().firstOrNull() ?: return@map
            tokenDao.delete(activeId)
        }
    }

    override suspend fun setExperience(experience: ExperienceEnum): Result<Boolean> {
        val response = api.updateExperience(
            com.grippo.services.backend.dto.user.ExperienceBody(experience = experience.key)
        )

        return response.map { dto ->
            val user = dto.toEntityOrNull() ?: return@map false
            userDao.insertOrUpdate(user)
            true
        }
    }
}
