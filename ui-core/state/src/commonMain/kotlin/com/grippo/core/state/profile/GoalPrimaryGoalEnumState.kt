package com.grippo.core.state.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_primary_build_muscle
import com.grippo.design.resources.provider.goal_primary_general_fitness
import com.grippo.design.resources.provider.goal_primary_get_stronger
import com.grippo.design.resources.provider.goal_primary_lose_fat
import com.grippo.design.resources.provider.goal_primary_maintain
import com.grippo.design.resources.provider.goal_primary_return_to_training

@Immutable
public enum class GoalPrimaryGoalEnumState {
    BUILD_MUSCLE,
    GET_STRONGER,
    LOSE_FAT,
    RETURN_TO_TRAINING,
    MAINTAIN,
    GENERAL_FITNESS;

    @Composable
    public fun label(): String {
        return when (this) {
            BUILD_MUSCLE -> AppTokens.strings.res(Res.string.goal_primary_build_muscle)
            GET_STRONGER -> AppTokens.strings.res(Res.string.goal_primary_get_stronger)
            LOSE_FAT -> AppTokens.strings.res(Res.string.goal_primary_lose_fat)
            RETURN_TO_TRAINING -> AppTokens.strings.res(Res.string.goal_primary_return_to_training)
            MAINTAIN -> AppTokens.strings.res(Res.string.goal_primary_maintain)
            GENERAL_FITNESS -> AppTokens.strings.res(Res.string.goal_primary_general_fitness)
        }
    }
}
