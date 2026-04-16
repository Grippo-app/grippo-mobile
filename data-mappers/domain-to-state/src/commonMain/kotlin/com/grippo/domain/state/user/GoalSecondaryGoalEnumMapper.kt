package com.grippo.domain.state.user

import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.data.features.api.goal.models.GoalSecondaryGoalEnum

public fun GoalSecondaryGoalEnum.toState(): GoalSecondaryGoalEnumState {
    return when (this) {
        GoalSecondaryGoalEnum.BUILD_MUSCLE -> GoalSecondaryGoalEnumState.BUILD_MUSCLE
        GoalSecondaryGoalEnum.GET_STRONGER -> GoalSecondaryGoalEnumState.GET_STRONGER
        GoalSecondaryGoalEnum.LOSE_FAT -> GoalSecondaryGoalEnumState.LOSE_FAT
        GoalSecondaryGoalEnum.RETURN_TO_TRAINING -> GoalSecondaryGoalEnumState.RETURN_TO_TRAINING
        GoalSecondaryGoalEnum.MAINTAIN -> GoalSecondaryGoalEnumState.MAINTAIN
        GoalSecondaryGoalEnum.GENERAL_FITNESS -> GoalSecondaryGoalEnumState.GENERAL_FITNESS
        GoalSecondaryGoalEnum.CONSISTENCY -> GoalSecondaryGoalEnumState.CONSISTENCY
    }
}
