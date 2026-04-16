package com.grippo.data.features.api.goal

import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.goal.models.SetGoal
import kotlinx.coroutines.flow.Flow

public interface GoalFeature {
    public fun observeGoal(): Flow<Goal?>
    public suspend fun getGoal(): Result<Boolean>
    public suspend fun setGoal(goal: SetGoal): Result<Boolean>
}