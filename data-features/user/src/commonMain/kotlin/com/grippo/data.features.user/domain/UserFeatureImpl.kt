package com.grippo.data.features.user.domain

import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import kotlinx.coroutines.flow.Flow

internal class UserFeatureImpl(
    private val repository: UserRepository
) : UserFeature {

    override fun observeUser(): Flow<User?> {
        return repository.observeUser()
    }

    override suspend fun getUser(): Result<Unit> {
        return repository.getUser()
    }
}