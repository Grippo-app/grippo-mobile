package com.grippo.data.features.api.user

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.data.features.api.user.models.User
import kotlinx.coroutines.flow.Flow

public interface UserFeature {

    public fun observeUser(): Flow<User?>

    public suspend fun getUser(): Result<Boolean>

    public suspend fun createProfile(profile: CreateUserProfile): Result<Boolean>

    public suspend fun deleteProfile(): Result<Unit>

    public suspend fun setExperience(experience: ExperienceEnum): Result<Boolean>

    public suspend fun setHeight(height: Int): Result<Boolean>
}
