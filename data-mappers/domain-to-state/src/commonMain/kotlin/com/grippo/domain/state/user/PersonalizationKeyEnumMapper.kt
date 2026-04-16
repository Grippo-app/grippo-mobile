package com.grippo.domain.state.user

import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.data.features.api.goal.models.PersonalizationKeyEnum

public fun PersonalizationKeyEnum.toState(): PersonalizationKeyEnumState {
    return when (this) {
        PersonalizationKeyEnum.LOW_FREQUENCY -> PersonalizationKeyEnumState.LOW_FREQUENCY
        PersonalizationKeyEnum.MODERATE_FREQUENCY -> PersonalizationKeyEnumState.MODERATE_FREQUENCY
        PersonalizationKeyEnum.HIGH_FREQUENCY -> PersonalizationKeyEnumState.HIGH_FREQUENCY
        PersonalizationKeyEnum.WEEKENDS_ONLY -> PersonalizationKeyEnumState.WEEKENDS_ONLY
        PersonalizationKeyEnum.WEEKDAYS_ONLY -> PersonalizationKeyEnumState.WEEKDAYS_ONLY
        PersonalizationKeyEnum.FLEXIBLE_SCHEDULE -> PersonalizationKeyEnumState.FLEXIBLE_SCHEDULE
        PersonalizationKeyEnum.INCONSISTENT_SCHEDULE -> PersonalizationKeyEnumState.INCONSISTENT_SCHEDULE
        PersonalizationKeyEnum.MINUTES_30_45 -> PersonalizationKeyEnumState.MINUTES_30_45
        PersonalizationKeyEnum.MINUTES_45_60 -> PersonalizationKeyEnumState.MINUTES_45_60
        PersonalizationKeyEnum.MINUTES_60_90 -> PersonalizationKeyEnumState.MINUTES_60_90
        PersonalizationKeyEnum.MINUTES_90_120 -> PersonalizationKeyEnumState.MINUTES_90_120
        PersonalizationKeyEnum.MINUTES_120_PLUS -> PersonalizationKeyEnumState.MINUTES_120_PLUS
        PersonalizationKeyEnum.FLEXIBLE_DURATION -> PersonalizationKeyEnumState.FLEXIBLE_DURATION
        PersonalizationKeyEnum.LOW_ENERGY_DAYS -> PersonalizationKeyEnumState.LOW_ENERGY_DAYS
        PersonalizationKeyEnum.STRESSFUL_PERIOD -> PersonalizationKeyEnumState.STRESSFUL_PERIOD
        PersonalizationKeyEnum.POOR_SLEEP -> PersonalizationKeyEnumState.POOR_SLEEP
        PersonalizationKeyEnum.TRAVELS_OFTEN -> PersonalizationKeyEnumState.TRAVELS_OFTEN
        PersonalizationKeyEnum.JOINT_FRIENDLY -> PersonalizationKeyEnumState.JOINT_FRIENDLY
        PersonalizationKeyEnum.LOW_IMPACT -> PersonalizationKeyEnumState.LOW_IMPACT
        PersonalizationKeyEnum.BACK_CARE -> PersonalizationKeyEnumState.BACK_CARE
        PersonalizationKeyEnum.KNEE_CARE -> PersonalizationKeyEnumState.KNEE_CARE
        PersonalizationKeyEnum.SHOULDER_CARE -> PersonalizationKeyEnumState.SHOULDER_CARE
        PersonalizationKeyEnum.WRIST_CARE -> PersonalizationKeyEnumState.WRIST_CARE
        PersonalizationKeyEnum.RETURNING_AFTER_BREAK -> PersonalizationKeyEnumState.RETURNING_AFTER_BREAK
        PersonalizationKeyEnum.RECOVERING_FROM_INJURY -> PersonalizationKeyEnumState.RECOVERING_FROM_INJURY
        PersonalizationKeyEnum.RECOVERY_FOCUS -> PersonalizationKeyEnumState.RECOVERY_FOCUS
        PersonalizationKeyEnum.MOBILITY_FOCUS -> PersonalizationKeyEnumState.MOBILITY_FOCUS
        PersonalizationKeyEnum.WARMUP_FOCUS -> PersonalizationKeyEnumState.WARMUP_FOCUS
        PersonalizationKeyEnum.LIMITED_RANGE_OF_MOTION -> PersonalizationKeyEnumState.LIMITED_RANGE_OF_MOTION
        PersonalizationKeyEnum.FATIGUE_MANAGEMENT -> PersonalizationKeyEnumState.FATIGUE_MANAGEMENT
        PersonalizationKeyEnum.AVOID_HIGH_INTENSITY -> PersonalizationKeyEnumState.AVOID_HIGH_INTENSITY
        PersonalizationKeyEnum.AVOID_HEAVY_LOADING -> PersonalizationKeyEnumState.AVOID_HEAVY_LOADING
        PersonalizationKeyEnum.AVOID_DEEP_SQUATS -> PersonalizationKeyEnumState.AVOID_DEEP_SQUATS
        PersonalizationKeyEnum.AVOID_LUNGES -> PersonalizationKeyEnumState.AVOID_LUNGES
        PersonalizationKeyEnum.AVOID_OVERHEAD_WORK -> PersonalizationKeyEnumState.AVOID_OVERHEAD_WORK
        PersonalizationKeyEnum.AVOID_HEAVY_SPINAL_LOADING -> PersonalizationKeyEnumState.AVOID_HEAVY_SPINAL_LOADING
        PersonalizationKeyEnum.AVOID_TWISTING -> PersonalizationKeyEnumState.AVOID_TWISTING
        PersonalizationKeyEnum.AVOID_FLOOR_WORK -> PersonalizationKeyEnumState.AVOID_FLOOR_WORK
        PersonalizationKeyEnum.NEEDS_SUPPORTED_MOVEMENTS -> PersonalizationKeyEnumState.NEEDS_SUPPORTED_MOVEMENTS
        PersonalizationKeyEnum.SIMPLE_PLAN -> PersonalizationKeyEnumState.SIMPLE_PLAN
        PersonalizationKeyEnum.MORE_VARIETY -> PersonalizationKeyEnumState.MORE_VARIETY
        PersonalizationKeyEnum.LOWER_VOLUME -> PersonalizationKeyEnumState.LOWER_VOLUME
        PersonalizationKeyEnum.HIGHER_VOLUME -> PersonalizationKeyEnumState.HIGHER_VOLUME
        PersonalizationKeyEnum.GRADUAL_PROGRESSION -> PersonalizationKeyEnumState.GRADUAL_PROGRESSION
        PersonalizationKeyEnum.AGGRESSIVE_PROGRESSION -> PersonalizationKeyEnumState.AGGRESSIVE_PROGRESSION
        PersonalizationKeyEnum.BEGINNER_FRIENDLY -> PersonalizationKeyEnumState.BEGINNER_FRIENDLY
        PersonalizationKeyEnum.EASY_TO_FOLLOW -> PersonalizationKeyEnumState.EASY_TO_FOLLOW
        PersonalizationKeyEnum.NO_COMPLEX_MOVEMENTS -> PersonalizationKeyEnumState.NO_COMPLEX_MOVEMENTS
        PersonalizationKeyEnum.FULL_BODY_PREFERENCE -> PersonalizationKeyEnumState.FULL_BODY_PREFERENCE
        PersonalizationKeyEnum.SPLIT_ROUTINE_PREFERENCE -> PersonalizationKeyEnumState.SPLIT_ROUTINE_PREFERENCE
        PersonalizationKeyEnum.UPPER_LOWER_PREFERENCE -> PersonalizationKeyEnumState.UPPER_LOWER_PREFERENCE
    }
}
