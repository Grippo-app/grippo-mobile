package com.grippo.data.features.api.user

import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.data.features.api.user.models.User
import kotlinx.coroutines.flow.Flow

public interface UserFeature {
    public fun observeUser(): Flow<User?>

    public suspend fun getUser(): Result<Boolean>
    public suspend fun createProfile(profile: CreateUserProfile): Result<Boolean>
}
