package com.grippo.data.features.goal.domain

import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.goal.models.SetGoal
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single(binds = [GoalFeature::class])
internal class GoalFeatureImpl(
    private val repository: GoalRepository
) : GoalFeature {

    override fun observeGoal(): Flow<Goal?> {
        return repository.observeGoal()
    }

    override suspend fun getGoal(): Result<Boolean> {
        return repository.getGoal()
    }

    override suspend fun setGoal(goal: SetGoal): Result<Boolean> {
        return repository.setGoal(goal)
    }
}
