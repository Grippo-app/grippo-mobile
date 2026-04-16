package com.grippo.state.domain.goal

import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.data.features.api.goal.models.GoalPrimaryGoalEnum

public fun GoalPrimaryGoalEnumState.toDomain(): GoalPrimaryGoalEnum {
    return when (this) {
        GoalPrimaryGoalEnumState.BUILD_MUSCLE -> GoalPrimaryGoalEnum.BUILD_MUSCLE
        GoalPrimaryGoalEnumState.GET_STRONGER -> GoalPrimaryGoalEnum.GET_STRONGER
        GoalPrimaryGoalEnumState.LOSE_FAT -> GoalPrimaryGoalEnum.LOSE_FAT
        GoalPrimaryGoalEnumState.RETURN_TO_TRAINING -> GoalPrimaryGoalEnum.RETURN_TO_TRAINING
        GoalPrimaryGoalEnumState.MAINTAIN -> GoalPrimaryGoalEnum.MAINTAIN
        GoalPrimaryGoalEnumState.GENERAL_FITNESS -> GoalPrimaryGoalEnum.GENERAL_FITNESS
    }
}
