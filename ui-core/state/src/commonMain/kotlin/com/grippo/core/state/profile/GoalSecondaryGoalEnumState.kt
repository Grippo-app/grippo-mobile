package com.grippo.core.state.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_secondary_build_muscle
import com.grippo.design.resources.provider.goal_secondary_build_muscle_desc
import com.grippo.design.resources.provider.goal_secondary_consistency
import com.grippo.design.resources.provider.goal_secondary_consistency_desc
import com.grippo.design.resources.provider.goal_secondary_general_fitness
import com.grippo.design.resources.provider.goal_secondary_general_fitness_desc
import com.grippo.design.resources.provider.goal_secondary_get_stronger
import com.grippo.design.resources.provider.goal_secondary_get_stronger_desc
import com.grippo.design.resources.provider.goal_secondary_lose_fat
import com.grippo.design.resources.provider.goal_secondary_lose_fat_desc
import com.grippo.design.resources.provider.goal_secondary_maintain
import com.grippo.design.resources.provider.goal_secondary_maintain_desc
import com.grippo.design.resources.provider.goal_secondary_return_to_training
import com.grippo.design.resources.provider.goal_secondary_return_to_training_desc

@Immutable
public enum class GoalSecondaryGoalEnumState {
    BUILD_MUSCLE,
    GET_STRONGER,
    LOSE_FAT,
    RETURN_TO_TRAINING,
    MAINTAIN,
    GENERAL_FITNESS,
    CONSISTENCY;

    @Composable
    public fun label(): String {
        return when (this) {
            BUILD_MUSCLE -> AppTokens.strings.res(Res.string.goal_secondary_build_muscle)
            GET_STRONGER -> AppTokens.strings.res(Res.string.goal_secondary_get_stronger)
            LOSE_FAT -> AppTokens.strings.res(Res.string.goal_secondary_lose_fat)
            RETURN_TO_TRAINING -> AppTokens.strings.res(Res.string.goal_secondary_return_to_training)
            MAINTAIN -> AppTokens.strings.res(Res.string.goal_secondary_maintain)
            GENERAL_FITNESS -> AppTokens.strings.res(Res.string.goal_secondary_general_fitness)
            CONSISTENCY -> AppTokens.strings.res(Res.string.goal_secondary_consistency)
        }
    }

    @Composable
    public fun description(): String {
        return when (this) {
            BUILD_MUSCLE -> AppTokens.strings.res(Res.string.goal_secondary_build_muscle_desc)
            GET_STRONGER -> AppTokens.strings.res(Res.string.goal_secondary_get_stronger_desc)
            LOSE_FAT -> AppTokens.strings.res(Res.string.goal_secondary_lose_fat_desc)
            RETURN_TO_TRAINING -> AppTokens.strings.res(Res.string.goal_secondary_return_to_training_desc)
            MAINTAIN -> AppTokens.strings.res(Res.string.goal_secondary_maintain_desc)
            GENERAL_FITNESS -> AppTokens.strings.res(Res.string.goal_secondary_general_fitness_desc)
            CONSISTENCY -> AppTokens.strings.res(Res.string.goal_secondary_consistency_desc)
        }
    }
}
