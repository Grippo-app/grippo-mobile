package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_fit_rule_build_muscle_weekly_sets_per_primary_group_title
import com.grippo.design.resources.provider.goal_fit_rule_build_muscle_weekly_frequency_per_primary_group_title
import com.grippo.design.resources.provider.goal_fit_rule_build_muscle_load_progression_title
import com.grippo.design.resources.provider.goal_fit_rule_build_muscle_weekly_frequency_title
import com.grippo.design.resources.provider.goal_fit_rule_get_stronger_strength_rep_range_share_title
import com.grippo.design.resources.provider.goal_fit_rule_get_stronger_compound_set_share_title
import com.grippo.design.resources.provider.goal_fit_rule_get_stronger_strength_progression_title
import com.grippo.design.resources.provider.goal_fit_rule_get_stronger_weekly_frequency_title
import com.grippo.design.resources.provider.goal_fit_rule_maintain_group_coverage_title
import com.grippo.design.resources.provider.goal_fit_rule_maintain_weekly_frequency_title
import com.grippo.design.resources.provider.goal_fit_rule_maintain_weekly_volume_stability_title
import com.grippo.design.resources.provider.goal_fit_rule_general_fitness_group_coverage_title
import com.grippo.design.resources.provider.goal_fit_rule_general_fitness_weekly_frequency_title
import com.grippo.design.resources.provider.goal_fit_rule_lose_fat_session_adherence_title
import com.grippo.design.resources.provider.goal_fit_rule_lose_fat_resistance_presence_title
import com.grippo.design.resources.provider.goal_fit_rule_lose_fat_weekly_volume_trend_title
import com.grippo.design.resources.provider.goal_fit_rule_return_to_training_weekly_frequency_title
import com.grippo.design.resources.provider.goal_fit_rule_return_to_training_load_ramp_gentle_title

@Immutable
public enum class GoalFitRuleState {
    BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
    BUILD_MUSCLE_WEEKLY_FREQUENCY_PER_PRIMARY_GROUP,
    BUILD_MUSCLE_LOAD_PROGRESSION,
    BUILD_MUSCLE_WEEKLY_FREQUENCY,
    GET_STRONGER_STRENGTH_REP_RANGE_SHARE,
    GET_STRONGER_COMPOUND_SET_SHARE,
    GET_STRONGER_STRENGTH_PROGRESSION,
    GET_STRONGER_WEEKLY_FREQUENCY,
    MAINTAIN_GROUP_COVERAGE,
    MAINTAIN_WEEKLY_FREQUENCY,
    MAINTAIN_WEEKLY_VOLUME_STABILITY,
    GENERAL_FITNESS_GROUP_COVERAGE,
    GENERAL_FITNESS_WEEKLY_FREQUENCY,
    LOSE_FAT_SESSION_ADHERENCE,
    LOSE_FAT_RESISTANCE_PRESENCE,
    LOSE_FAT_WEEKLY_VOLUME_TREND,
    RETURN_TO_TRAINING_WEEKLY_FREQUENCY,
    RETURN_TO_TRAINING_LOAD_RAMP_GENTLE;

    @Composable
    public fun title(): String = when (this) {
        BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP -> AppTokens.strings.res(Res.string.goal_fit_rule_build_muscle_weekly_sets_per_primary_group_title)
        BUILD_MUSCLE_WEEKLY_FREQUENCY_PER_PRIMARY_GROUP -> AppTokens.strings.res(Res.string.goal_fit_rule_build_muscle_weekly_frequency_per_primary_group_title)
        BUILD_MUSCLE_LOAD_PROGRESSION -> AppTokens.strings.res(Res.string.goal_fit_rule_build_muscle_load_progression_title)
        BUILD_MUSCLE_WEEKLY_FREQUENCY -> AppTokens.strings.res(Res.string.goal_fit_rule_build_muscle_weekly_frequency_title)
        GET_STRONGER_STRENGTH_REP_RANGE_SHARE -> AppTokens.strings.res(Res.string.goal_fit_rule_get_stronger_strength_rep_range_share_title)
        GET_STRONGER_COMPOUND_SET_SHARE -> AppTokens.strings.res(Res.string.goal_fit_rule_get_stronger_compound_set_share_title)
        GET_STRONGER_STRENGTH_PROGRESSION -> AppTokens.strings.res(Res.string.goal_fit_rule_get_stronger_strength_progression_title)
        GET_STRONGER_WEEKLY_FREQUENCY -> AppTokens.strings.res(Res.string.goal_fit_rule_get_stronger_weekly_frequency_title)
        MAINTAIN_GROUP_COVERAGE -> AppTokens.strings.res(Res.string.goal_fit_rule_maintain_group_coverage_title)
        MAINTAIN_WEEKLY_FREQUENCY -> AppTokens.strings.res(Res.string.goal_fit_rule_maintain_weekly_frequency_title)
        MAINTAIN_WEEKLY_VOLUME_STABILITY -> AppTokens.strings.res(Res.string.goal_fit_rule_maintain_weekly_volume_stability_title)
        GENERAL_FITNESS_GROUP_COVERAGE -> AppTokens.strings.res(Res.string.goal_fit_rule_general_fitness_group_coverage_title)
        GENERAL_FITNESS_WEEKLY_FREQUENCY -> AppTokens.strings.res(Res.string.goal_fit_rule_general_fitness_weekly_frequency_title)
        LOSE_FAT_SESSION_ADHERENCE -> AppTokens.strings.res(Res.string.goal_fit_rule_lose_fat_session_adherence_title)
        LOSE_FAT_RESISTANCE_PRESENCE -> AppTokens.strings.res(Res.string.goal_fit_rule_lose_fat_resistance_presence_title)
        LOSE_FAT_WEEKLY_VOLUME_TREND -> AppTokens.strings.res(Res.string.goal_fit_rule_lose_fat_weekly_volume_trend_title)
        RETURN_TO_TRAINING_WEEKLY_FREQUENCY -> AppTokens.strings.res(Res.string.goal_fit_rule_return_to_training_weekly_frequency_title)
        RETURN_TO_TRAINING_LOAD_RAMP_GENTLE -> AppTokens.strings.res(Res.string.goal_fit_rule_return_to_training_load_ramp_gentle_title)
    }
}
