package com.grippo.core.state.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.personalization_aggressive_progression
import com.grippo.design.resources.provider.personalization_avoid_deep_squats
import com.grippo.design.resources.provider.personalization_avoid_floor_work
import com.grippo.design.resources.provider.personalization_avoid_heavy_loading
import com.grippo.design.resources.provider.personalization_avoid_heavy_spinal_loading
import com.grippo.design.resources.provider.personalization_avoid_high_intensity
import com.grippo.design.resources.provider.personalization_avoid_lunges
import com.grippo.design.resources.provider.personalization_avoid_overhead_work
import com.grippo.design.resources.provider.personalization_avoid_twisting
import com.grippo.design.resources.provider.personalization_back_care
import com.grippo.design.resources.provider.personalization_beginner_friendly
import com.grippo.design.resources.provider.personalization_easy_to_follow
import com.grippo.design.resources.provider.personalization_fatigue_management
import com.grippo.design.resources.provider.personalization_flexible_duration
import com.grippo.design.resources.provider.personalization_flexible_schedule
import com.grippo.design.resources.provider.personalization_full_body_preference
import com.grippo.design.resources.provider.personalization_gradual_progression
import com.grippo.design.resources.provider.personalization_high_frequency
import com.grippo.design.resources.provider.personalization_higher_volume
import com.grippo.design.resources.provider.personalization_inconsistent_schedule
import com.grippo.design.resources.provider.personalization_joint_friendly
import com.grippo.design.resources.provider.personalization_knee_care
import com.grippo.design.resources.provider.personalization_limited_range_of_motion
import com.grippo.design.resources.provider.personalization_low_energy_days
import com.grippo.design.resources.provider.personalization_low_frequency
import com.grippo.design.resources.provider.personalization_low_impact
import com.grippo.design.resources.provider.personalization_lower_volume
import com.grippo.design.resources.provider.personalization_minutes_120_plus
import com.grippo.design.resources.provider.personalization_minutes_30_45
import com.grippo.design.resources.provider.personalization_minutes_45_60
import com.grippo.design.resources.provider.personalization_minutes_60_90
import com.grippo.design.resources.provider.personalization_minutes_90_120
import com.grippo.design.resources.provider.personalization_mobility_focus
import com.grippo.design.resources.provider.personalization_moderate_frequency
import com.grippo.design.resources.provider.personalization_more_variety
import com.grippo.design.resources.provider.personalization_needs_supported_movements
import com.grippo.design.resources.provider.personalization_no_complex_movements
import com.grippo.design.resources.provider.personalization_poor_sleep
import com.grippo.design.resources.provider.personalization_recovering_from_injury
import com.grippo.design.resources.provider.personalization_recovery_focus
import com.grippo.design.resources.provider.personalization_returning_after_break
import com.grippo.design.resources.provider.personalization_shoulder_care
import com.grippo.design.resources.provider.personalization_simple_plan
import com.grippo.design.resources.provider.personalization_split_routine_preference
import com.grippo.design.resources.provider.personalization_stressful_period
import com.grippo.design.resources.provider.personalization_travels_often
import com.grippo.design.resources.provider.personalization_upper_lower_preference
import com.grippo.design.resources.provider.personalization_warmup_focus
import com.grippo.design.resources.provider.personalization_weekdays_only
import com.grippo.design.resources.provider.personalization_weekends_only
import com.grippo.design.resources.provider.personalization_wrist_care

@Immutable
public enum class PersonalizationKeyEnumState {
    LOW_FREQUENCY,
    MODERATE_FREQUENCY,
    HIGH_FREQUENCY,
    WEEKENDS_ONLY,
    WEEKDAYS_ONLY,
    FLEXIBLE_SCHEDULE,
    INCONSISTENT_SCHEDULE,
    MINUTES_30_45,
    MINUTES_45_60,
    MINUTES_60_90,
    MINUTES_90_120,
    MINUTES_120_PLUS,
    FLEXIBLE_DURATION,
    LOW_ENERGY_DAYS,
    STRESSFUL_PERIOD,
    POOR_SLEEP,
    TRAVELS_OFTEN,
    JOINT_FRIENDLY,
    LOW_IMPACT,
    BACK_CARE,
    KNEE_CARE,
    SHOULDER_CARE,
    WRIST_CARE,
    RETURNING_AFTER_BREAK,
    RECOVERING_FROM_INJURY,
    RECOVERY_FOCUS,
    MOBILITY_FOCUS,
    WARMUP_FOCUS,
    LIMITED_RANGE_OF_MOTION,
    FATIGUE_MANAGEMENT,
    AVOID_HIGH_INTENSITY,
    AVOID_HEAVY_LOADING,
    AVOID_DEEP_SQUATS,
    AVOID_LUNGES,
    AVOID_OVERHEAD_WORK,
    AVOID_HEAVY_SPINAL_LOADING,
    AVOID_TWISTING,
    AVOID_FLOOR_WORK,
    NEEDS_SUPPORTED_MOVEMENTS,
    SIMPLE_PLAN,
    MORE_VARIETY,
    LOWER_VOLUME,
    HIGHER_VOLUME,
    GRADUAL_PROGRESSION,
    AGGRESSIVE_PROGRESSION,
    BEGINNER_FRIENDLY,
    EASY_TO_FOLLOW,
    NO_COMPLEX_MOVEMENTS,
    FULL_BODY_PREFERENCE,
    SPLIT_ROUTINE_PREFERENCE,
    UPPER_LOWER_PREFERENCE;

    @Composable
    public fun label(): String {
        return when (this) {
            LOW_FREQUENCY -> AppTokens.strings.res(Res.string.personalization_low_frequency)
            MODERATE_FREQUENCY -> AppTokens.strings.res(Res.string.personalization_moderate_frequency)
            HIGH_FREQUENCY -> AppTokens.strings.res(Res.string.personalization_high_frequency)
            WEEKENDS_ONLY -> AppTokens.strings.res(Res.string.personalization_weekends_only)
            WEEKDAYS_ONLY -> AppTokens.strings.res(Res.string.personalization_weekdays_only)
            FLEXIBLE_SCHEDULE -> AppTokens.strings.res(Res.string.personalization_flexible_schedule)
            INCONSISTENT_SCHEDULE -> AppTokens.strings.res(Res.string.personalization_inconsistent_schedule)
            MINUTES_30_45 -> AppTokens.strings.res(Res.string.personalization_minutes_30_45)
            MINUTES_45_60 -> AppTokens.strings.res(Res.string.personalization_minutes_45_60)
            MINUTES_60_90 -> AppTokens.strings.res(Res.string.personalization_minutes_60_90)
            MINUTES_90_120 -> AppTokens.strings.res(Res.string.personalization_minutes_90_120)
            MINUTES_120_PLUS -> AppTokens.strings.res(Res.string.personalization_minutes_120_plus)
            FLEXIBLE_DURATION -> AppTokens.strings.res(Res.string.personalization_flexible_duration)
            LOW_ENERGY_DAYS -> AppTokens.strings.res(Res.string.personalization_low_energy_days)
            STRESSFUL_PERIOD -> AppTokens.strings.res(Res.string.personalization_stressful_period)
            POOR_SLEEP -> AppTokens.strings.res(Res.string.personalization_poor_sleep)
            TRAVELS_OFTEN -> AppTokens.strings.res(Res.string.personalization_travels_often)
            JOINT_FRIENDLY -> AppTokens.strings.res(Res.string.personalization_joint_friendly)
            LOW_IMPACT -> AppTokens.strings.res(Res.string.personalization_low_impact)
            BACK_CARE -> AppTokens.strings.res(Res.string.personalization_back_care)
            KNEE_CARE -> AppTokens.strings.res(Res.string.personalization_knee_care)
            SHOULDER_CARE -> AppTokens.strings.res(Res.string.personalization_shoulder_care)
            WRIST_CARE -> AppTokens.strings.res(Res.string.personalization_wrist_care)
            RETURNING_AFTER_BREAK -> AppTokens.strings.res(Res.string.personalization_returning_after_break)
            RECOVERING_FROM_INJURY -> AppTokens.strings.res(Res.string.personalization_recovering_from_injury)
            RECOVERY_FOCUS -> AppTokens.strings.res(Res.string.personalization_recovery_focus)
            MOBILITY_FOCUS -> AppTokens.strings.res(Res.string.personalization_mobility_focus)
            WARMUP_FOCUS -> AppTokens.strings.res(Res.string.personalization_warmup_focus)
            LIMITED_RANGE_OF_MOTION -> AppTokens.strings.res(Res.string.personalization_limited_range_of_motion)
            FATIGUE_MANAGEMENT -> AppTokens.strings.res(Res.string.personalization_fatigue_management)
            AVOID_HIGH_INTENSITY -> AppTokens.strings.res(Res.string.personalization_avoid_high_intensity)
            AVOID_HEAVY_LOADING -> AppTokens.strings.res(Res.string.personalization_avoid_heavy_loading)
            AVOID_DEEP_SQUATS -> AppTokens.strings.res(Res.string.personalization_avoid_deep_squats)
            AVOID_LUNGES -> AppTokens.strings.res(Res.string.personalization_avoid_lunges)
            AVOID_OVERHEAD_WORK -> AppTokens.strings.res(Res.string.personalization_avoid_overhead_work)
            AVOID_HEAVY_SPINAL_LOADING -> AppTokens.strings.res(Res.string.personalization_avoid_heavy_spinal_loading)
            AVOID_TWISTING -> AppTokens.strings.res(Res.string.personalization_avoid_twisting)
            AVOID_FLOOR_WORK -> AppTokens.strings.res(Res.string.personalization_avoid_floor_work)
            NEEDS_SUPPORTED_MOVEMENTS -> AppTokens.strings.res(Res.string.personalization_needs_supported_movements)
            SIMPLE_PLAN -> AppTokens.strings.res(Res.string.personalization_simple_plan)
            MORE_VARIETY -> AppTokens.strings.res(Res.string.personalization_more_variety)
            LOWER_VOLUME -> AppTokens.strings.res(Res.string.personalization_lower_volume)
            HIGHER_VOLUME -> AppTokens.strings.res(Res.string.personalization_higher_volume)
            GRADUAL_PROGRESSION -> AppTokens.strings.res(Res.string.personalization_gradual_progression)
            AGGRESSIVE_PROGRESSION -> AppTokens.strings.res(Res.string.personalization_aggressive_progression)
            BEGINNER_FRIENDLY -> AppTokens.strings.res(Res.string.personalization_beginner_friendly)
            EASY_TO_FOLLOW -> AppTokens.strings.res(Res.string.personalization_easy_to_follow)
            NO_COMPLEX_MOVEMENTS -> AppTokens.strings.res(Res.string.personalization_no_complex_movements)
            FULL_BODY_PREFERENCE -> AppTokens.strings.res(Res.string.personalization_full_body_preference)
            SPLIT_ROUTINE_PREFERENCE -> AppTokens.strings.res(Res.string.personalization_split_routine_preference)
            UPPER_LOWER_PREFERENCE -> AppTokens.strings.res(Res.string.personalization_upper_lower_preference)
        }
    }
}
