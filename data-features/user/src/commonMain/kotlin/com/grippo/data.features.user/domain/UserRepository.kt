package com.grippo.data.features.user.domain

import com.grippo.data.features.api.user.models.User
import kotlinx.coroutines.flow.Flow

internal interface UserRepository {
    fun observeUser(): Flow<User?>

    suspend fun getUser(): Result<Unit>
    suspend fun setExcludedMuscle(id: String): Result<Unit>
    suspend fun deleteExcludedMuscle(id: String): Result<Unit>
    suspend fun setExcludedEquipment(id: String): Result<Unit>
    suspend fun deleteExcludedEquipment(id: String): Result<Unit>
}