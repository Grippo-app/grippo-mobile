package com.grippo.data.features.api.user

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.data.features.api.user.models.User
import kotlinx.coroutines.flow.Flow

public interface UserFeature {

    public fun observeUser(): Flow<User?>

    public suspend fun getUser(): Boolean

    public suspend fun createProfile(profile: CreateUserProfile): Unit

    public suspend fun deleteProfile(): Unit

    public suspend fun setExperience(experience: ExperienceEnum): Unit

    public suspend fun setHeight(height: Int): Unit
}
