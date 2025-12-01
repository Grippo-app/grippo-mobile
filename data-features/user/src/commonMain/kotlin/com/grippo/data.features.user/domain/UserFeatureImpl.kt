package com.grippo.data.features.user.domain

import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.data.features.api.user.models.User
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single(binds = [UserFeature::class])
internal class UserFeatureImpl(
    private val repository: UserRepository
) : UserFeature {

    override fun observeUser(): Flow<User?> {
        return repository.observeUser()
    }

    override suspend fun getUser(): Result<Boolean> {
        return repository.getUser()
    }

    override suspend fun createProfile(profile: CreateUserProfile): Result<Boolean> {
        return repository.createProfile(profile)
    }
}
