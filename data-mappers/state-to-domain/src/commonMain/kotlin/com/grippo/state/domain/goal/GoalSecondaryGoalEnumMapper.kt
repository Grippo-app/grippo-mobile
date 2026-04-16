package com.grippo.state.domain.goal

import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.data.features.api.goal.models.GoalSecondaryGoalEnum

public fun GoalSecondaryGoalEnumState.toDomain(): GoalSecondaryGoalEnum {
    return when (this) {
        GoalSecondaryGoalEnumState.BUILD_MUSCLE -> GoalSecondaryGoalEnum.BUILD_MUSCLE
        GoalSecondaryGoalEnumState.GET_STRONGER -> GoalSecondaryGoalEnum.GET_STRONGER
        GoalSecondaryGoalEnumState.LOSE_FAT -> GoalSecondaryGoalEnum.LOSE_FAT
        GoalSecondaryGoalEnumState.RETURN_TO_TRAINING -> GoalSecondaryGoalEnum.RETURN_TO_TRAINING
        GoalSecondaryGoalEnumState.MAINTAIN -> GoalSecondaryGoalEnum.MAINTAIN
        GoalSecondaryGoalEnumState.GENERAL_FITNESS -> GoalSecondaryGoalEnum.GENERAL_FITNESS
        GoalSecondaryGoalEnumState.CONSISTENCY -> GoalSecondaryGoalEnum.CONSISTENCY
    }
}
