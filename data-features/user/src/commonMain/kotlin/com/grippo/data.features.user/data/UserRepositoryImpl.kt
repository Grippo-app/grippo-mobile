package com.grippo.data.features.user.data

import com.grippo.core.error.provider.AppError
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.data.features.api.user.models.RoleEnum
import com.grippo.data.features.api.user.models.User
import com.grippo.data.features.user.domain.UserRepository
import com.grippo.domain.dto.user.toBody
import com.grippo.dto.entity.user.toEntityOrNull
import com.grippo.entity.domain.user.toDomain
import com.grippo.services.backend.GrippoApi
import com.grippo.services.backend.dto.user.HeightBody
import com.grippo.services.database.dao.TokenDao
import com.grippo.services.database.dao.UserActiveDao
import com.grippo.services.database.dao.UserDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [UserRepository::class])
internal class UserRepositoryImpl(
    private val api: GrippoApi,
    private val userDao: UserDao,
    private val tokenDao: TokenDao,
    private val userActiveDao: UserActiveDao
) : UserRepository {

    override fun observeUser(): Flow<User?> {
        return userActiveDao.get()
            .flatMapLatest { activeId ->
                if (activeId == null) flowOf(null)
                else userDao.getPackById(activeId)
            }.map { it?.toDomain() }
    }

    override suspend fun getUser(): Boolean {
        val userId = requireActiveUserId()
        val response = api.getUser()

        if (response.id != userId || response.email == null || RoleEnum.of(response.role) == null ||
            response.createdAt == null || response.updatedAt == null
        ) {
            throw AppError.Network.IncompleteResponse()
        }
        if (response.profile == null) {
            if (userDao.getById(userId).firstOrNull() != null) {
                // Deleting this cached user would also delete the local training draft.
                throw AppError.Expected(
                    message = "The user profile is unavailable",
                    description = null,
                )
            }
            return false
        }

        val pack = response.toEntityOrNull() ?: throw AppError.Network.IncompleteResponse()
        pack.toDomain() ?: throw AppError.Network.IncompleteResponse()
        userDao.insertOrUpdate(pack)
        return true
    }

    override suspend fun createProfile(profile: CreateUserProfile): Unit {
        val userId = requireActiveUserId()
        val response = api.createProfile(profile.toBody())

        if (response.id != userId) {
            throw AppError.Network.IncompleteResponse()
        }

        val pack = response.toEntityOrNull() ?: throw AppError.Network.IncompleteResponse()
        pack.toDomain() ?: throw AppError.Network.IncompleteResponse()
        userDao.insertOrUpdate(pack)
    }

    override suspend fun deleteProfile(): Unit {
        val activeId = requireActiveUserId()
        api.deleteUser()
        tokenDao.delete(activeId)
    }

    override suspend fun setExperience(experience: ExperienceEnum): Unit {
        val userId = requireActiveUserId()
        val response = api.updateExperience(
            com.grippo.services.backend.dto.user.ExperienceBody(experience = experience.key)
        )

        if (response.id != userId) {
            throw AppError.Network.IncompleteResponse()
        }

        val pack = response.toEntityOrNull() ?: throw AppError.Network.IncompleteResponse()
        pack.toDomain() ?: throw AppError.Network.IncompleteResponse()
        userDao.insertOrUpdate(pack)
    }

    override suspend fun setHeight(height: Int): Unit {
        val userId = requireActiveUserId()
        val response = api.updateHeight(
            HeightBody(height = height)
        )

        if (response.id != userId) {
            throw AppError.Network.IncompleteResponse()
        }

        val pack = response.toEntityOrNull() ?: throw AppError.Network.IncompleteResponse()
        pack.toDomain() ?: throw AppError.Network.IncompleteResponse()
        userDao.insertOrUpdate(pack)
    }

    private suspend fun requireActiveUserId(): String {
        return userActiveDao.get().firstOrNull()
            ?: throw AppError.Expected(
                message = "An active user is required",
                description = null,
            )
    }
}
