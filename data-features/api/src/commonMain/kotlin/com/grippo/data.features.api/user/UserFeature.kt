package com.grippo.data.features.api.user

import com.grippo.data.features.api.user.models.User
import com.grippo.data.features.api.user.models.WeightHistory
import kotlinx.coroutines.flow.Flow

public interface UserFeature {
    public fun observeUser(): Flow<User>
    public fun observeWeightHistory(): Flow<List<WeightHistory>>
    public fun observeLastWeight(): Flow<WeightHistory>

    public fun syncUser(): Result<Unit>
    public fun syncWeightHistory(): Result<Unit>
    public fun updateWeight(value: Double): Result<Unit>
    public fun removeWeight(id: String): Result<Unit>
    public fun setExcludedMuscle(id: String): Result<Unit>
    public fun deleteExcludedMuscle(id: String): Result<Unit>
    public fun setExcludedEquipment(id: String): Result<Unit>
    public fun deleteExcludedEquipment(id: String): Result<Unit>
}