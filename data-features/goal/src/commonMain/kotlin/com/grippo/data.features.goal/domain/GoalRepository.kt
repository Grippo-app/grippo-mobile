package com.grippo.data.features.goal.domain

import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.goal.models.SetGoal
import kotlinx.coroutines.flow.Flow

internal interface GoalRepository {
    fun observeGoal(): Flow<Goal?>
    suspend fun getGoal(): Result<Boolean>
    suspend fun setGoal(goal: SetGoal): Result<Boolean>
}
