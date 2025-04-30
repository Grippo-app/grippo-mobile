package com.grippo.data.features.user.domain

import com.grippo.data.features.api.user.models.User
import kotlinx.coroutines.flow.Flow

internal interface UserRepository {
    fun observeUser(): Flow<User?>


    fun getUser(): Result<Unit>

    fun setExcludedMuscle(id: String): Result<Unit>
    fun deleteExcludedMuscle(id: String): Result<Unit>
    fun setExcludedEquipment(id: String): Result<Unit>
    fun deleteExcludedEquipment(id: String): Result<Unit>
}