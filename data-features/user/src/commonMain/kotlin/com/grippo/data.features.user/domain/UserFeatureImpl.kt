package com.grippo.data.features.user.domain

import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.data.features.api.user.models.WeightHistory
import kotlinx.coroutines.flow.Flow

internal class UserFeatureImpl(
    private val repository: UserRepository
) : UserFeature {
    override fun observeUser(): Flow<User?> {
        return repository.observeUser()
    }

    override fun observeWeightHistory(): Flow<List<WeightHistory>> {
        return repository.observeWeightHistory()
    }

    override fun observeLastWeight(): Flow<WeightHistory> {
        return repository.observeLastWeight()
    }

    override fun getUser(): Result<Unit> {
        return repository.getUser()
    }

    override fun getWeightHistory(): Result<Unit> {
        return repository.getWeightHistory()
    }

    override fun updateWeight(value: Double): Result<Unit> {
        return repository.updateWeight(value)
    }

    override fun removeWeight(id: String): Result<Unit> {
        return repository.removeWeight(id)
    }

    override fun setExcludedMuscle(id: String): Result<Unit> {
        return repository.setExcludedMuscle(id)
    }

    override fun deleteExcludedMuscle(id: String): Result<Unit> {
        return repository.deleteExcludedMuscle(id)
    }

    override fun setExcludedEquipment(id: String): Result<Unit> {
        return repository.setExcludedEquipment(id)
    }

    override fun deleteExcludedEquipment(id: String): Result<Unit> {
        return repository.deleteExcludedEquipment(id)
    }

}