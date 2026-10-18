package com.grippo.data.features.user.domain

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.data.features.api.user.models.User
import kotlinx.coroutines.flow.Flow

internal interface UserRepository {

    fun observeUser(): Flow<User?>

    suspend fun getUser(): Boolean

    suspend fun createProfile(profile: CreateUserProfile): Unit

    suspend fun deleteProfile(): Unit

    suspend fun setExperience(experience: ExperienceEnum): Unit

    suspend fun setHeight(height: Int): Unit
}
