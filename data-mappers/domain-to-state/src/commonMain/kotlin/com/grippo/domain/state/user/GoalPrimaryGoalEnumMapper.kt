package com.grippo.domain.state.user

import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.data.features.api.goal.models.GoalPrimaryGoalEnum

public fun GoalPrimaryGoalEnum.toState(): GoalPrimaryGoalEnumState {
    return when (this) {
        GoalPrimaryGoalEnum.BUILD_MUSCLE -> GoalPrimaryGoalEnumState.BUILD_MUSCLE
        GoalPrimaryGoalEnum.GET_STRONGER -> GoalPrimaryGoalEnumState.GET_STRONGER
        GoalPrimaryGoalEnum.LOSE_FAT -> GoalPrimaryGoalEnumState.LOSE_FAT
        GoalPrimaryGoalEnum.RETURN_TO_TRAINING -> GoalPrimaryGoalEnumState.RETURN_TO_TRAINING
        GoalPrimaryGoalEnum.MAINTAIN -> GoalPrimaryGoalEnumState.MAINTAIN
        GoalPrimaryGoalEnum.GENERAL_FITNESS -> GoalPrimaryGoalEnumState.GENERAL_FITNESS
    }
}
