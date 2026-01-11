package com.grippo.data.features.user.domain

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.data.features.api.user.models.User
import kotlinx.coroutines.flow.Flow

internal interface UserRepository {

    fun observeUser(): Flow<User?>

    suspend fun getUser(): Result<Boolean>

    suspend fun createProfile(profile: CreateUserProfile): Result<Boolean>

    suspend fun deleteProfile(): Result<Unit>

    suspend fun setExperience(experience: ExperienceEnum): Result<Boolean>
}
