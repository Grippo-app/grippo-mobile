package com.grippo.data.features.api.user

import com.grippo.data.features.api.user.models.User
import kotlinx.coroutines.flow.Flow

public interface UserFeature {
    public fun observeUser(): Flow<User?>

    public suspend fun getUser(): Result<Unit>
    public suspend fun setExcludedMuscle(id: String): Result<Unit>
    public suspend fun deleteExcludedMuscle(id: String): Result<Unit>
    public suspend fun setExcludedEquipment(id: String): Result<Unit>
    public suspend fun deleteExcludedEquipment(id: String): Result<Unit>
}