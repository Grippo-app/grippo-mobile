package com.grippo.state.domain.goal

import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.data.features.api.goal.models.PersonalizationKeyEnum

public fun PersonalizationKeyEnumState.toDomain(): PersonalizationKeyEnum {
    return when (this) {
        PersonalizationKeyEnumState.LOW_FREQUENCY -> PersonalizationKeyEnum.LOW_FREQUENCY
        PersonalizationKeyEnumState.MODERATE_FREQUENCY -> PersonalizationKeyEnum.MODERATE_FREQUENCY
        PersonalizationKeyEnumState.HIGH_FREQUENCY -> PersonalizationKeyEnum.HIGH_FREQUENCY
        PersonalizationKeyEnumState.WEEKENDS_ONLY -> PersonalizationKeyEnum.WEEKENDS_ONLY
        PersonalizationKeyEnumState.WEEKDAYS_ONLY -> PersonalizationKeyEnum.WEEKDAYS_ONLY
        PersonalizationKeyEnumState.FLEXIBLE_SCHEDULE -> PersonalizationKeyEnum.FLEXIBLE_SCHEDULE
        PersonalizationKeyEnumState.INCONSISTENT_SCHEDULE -> PersonalizationKeyEnum.INCONSISTENT_SCHEDULE
        PersonalizationKeyEnumState.MINUTES_30_45 -> PersonalizationKeyEnum.MINUTES_30_45
        PersonalizationKeyEnumState.MINUTES_45_60 -> PersonalizationKeyEnum.MINUTES_45_60
        PersonalizationKeyEnumState.MINUTES_60_90 -> PersonalizationKeyEnum.MINUTES_60_90
        PersonalizationKeyEnumState.MINUTES_90_120 -> PersonalizationKeyEnum.MINUTES_90_120
        PersonalizationKeyEnumState.MINUTES_120_PLUS -> PersonalizationKeyEnum.MINUTES_120_PLUS
        PersonalizationKeyEnumState.FLEXIBLE_DURATION -> PersonalizationKeyEnum.FLEXIBLE_DURATION
        PersonalizationKeyEnumState.LOW_ENERGY_DAYS -> PersonalizationKeyEnum.LOW_ENERGY_DAYS
        PersonalizationKeyEnumState.STRESSFUL_PERIOD -> PersonalizationKeyEnum.STRESSFUL_PERIOD
        PersonalizationKeyEnumState.POOR_SLEEP -> PersonalizationKeyEnum.POOR_SLEEP
        PersonalizationKeyEnumState.TRAVELS_OFTEN -> PersonalizationKeyEnum.TRAVELS_OFTEN
        PersonalizationKeyEnumState.JOINT_FRIENDLY -> PersonalizationKeyEnum.JOINT_FRIENDLY
        PersonalizationKeyEnumState.LOW_IMPACT -> PersonalizationKeyEnum.LOW_IMPACT
        PersonalizationKeyEnumState.BACK_CARE -> PersonalizationKeyEnum.BACK_CARE
        PersonalizationKeyEnumState.KNEE_CARE -> PersonalizationKeyEnum.KNEE_CARE
        PersonalizationKeyEnumState.SHOULDER_CARE -> PersonalizationKeyEnum.SHOULDER_CARE
        PersonalizationKeyEnumState.WRIST_CARE -> PersonalizationKeyEnum.WRIST_CARE
        PersonalizationKeyEnumState.RETURNING_AFTER_BREAK -> PersonalizationKeyEnum.RETURNING_AFTER_BREAK
        PersonalizationKeyEnumState.RECOVERING_FROM_INJURY -> PersonalizationKeyEnum.RECOVERING_FROM_INJURY
        PersonalizationKeyEnumState.RECOVERY_FOCUS -> PersonalizationKeyEnum.RECOVERY_FOCUS
        PersonalizationKeyEnumState.MOBILITY_FOCUS -> PersonalizationKeyEnum.MOBILITY_FOCUS
        PersonalizationKeyEnumState.WARMUP_FOCUS -> PersonalizationKeyEnum.WARMUP_FOCUS
        PersonalizationKeyEnumState.LIMITED_RANGE_OF_MOTION -> PersonalizationKeyEnum.LIMITED_RANGE_OF_MOTION
        PersonalizationKeyEnumState.FATIGUE_MANAGEMENT -> PersonalizationKeyEnum.FATIGUE_MANAGEMENT
        PersonalizationKeyEnumState.AVOID_HIGH_INTENSITY -> PersonalizationKeyEnum.AVOID_HIGH_INTENSITY
        PersonalizationKeyEnumState.AVOID_HEAVY_LOADING -> PersonalizationKeyEnum.AVOID_HEAVY_LOADING
        PersonalizationKeyEnumState.AVOID_DEEP_SQUATS -> PersonalizationKeyEnum.AVOID_DEEP_SQUATS
        PersonalizationKeyEnumState.AVOID_LUNGES -> PersonalizationKeyEnum.AVOID_LUNGES
        PersonalizationKeyEnumState.AVOID_OVERHEAD_WORK -> PersonalizationKeyEnum.AVOID_OVERHEAD_WORK
        PersonalizationKeyEnumState.AVOID_HEAVY_SPINAL_LOADING -> PersonalizationKeyEnum.AVOID_HEAVY_SPINAL_LOADING
        PersonalizationKeyEnumState.AVOID_TWISTING -> PersonalizationKeyEnum.AVOID_TWISTING
        PersonalizationKeyEnumState.AVOID_FLOOR_WORK -> PersonalizationKeyEnum.AVOID_FLOOR_WORK
        PersonalizationKeyEnumState.NEEDS_SUPPORTED_MOVEMENTS -> PersonalizationKeyEnum.NEEDS_SUPPORTED_MOVEMENTS
        PersonalizationKeyEnumState.SIMPLE_PLAN -> PersonalizationKeyEnum.SIMPLE_PLAN
        PersonalizationKeyEnumState.MORE_VARIETY -> PersonalizationKeyEnum.MORE_VARIETY
        PersonalizationKeyEnumState.LOWER_VOLUME -> PersonalizationKeyEnum.LOWER_VOLUME
        PersonalizationKeyEnumState.HIGHER_VOLUME -> PersonalizationKeyEnum.HIGHER_VOLUME
        PersonalizationKeyEnumState.GRADUAL_PROGRESSION -> PersonalizationKeyEnum.GRADUAL_PROGRESSION
        PersonalizationKeyEnumState.AGGRESSIVE_PROGRESSION -> PersonalizationKeyEnum.AGGRESSIVE_PROGRESSION
        PersonalizationKeyEnumState.BEGINNER_FRIENDLY -> PersonalizationKeyEnum.BEGINNER_FRIENDLY
        PersonalizationKeyEnumState.EASY_TO_FOLLOW -> PersonalizationKeyEnum.EASY_TO_FOLLOW
        PersonalizationKeyEnumState.NO_COMPLEX_MOVEMENTS -> PersonalizationKeyEnum.NO_COMPLEX_MOVEMENTS
        PersonalizationKeyEnumState.FULL_BODY_PREFERENCE -> PersonalizationKeyEnum.FULL_BODY_PREFERENCE
        PersonalizationKeyEnumState.SPLIT_ROUTINE_PREFERENCE -> PersonalizationKeyEnum.SPLIT_ROUTINE_PREFERENCE
        PersonalizationKeyEnumState.UPPER_LOWER_PREFERENCE -> PersonalizationKeyEnum.UPPER_LOWER_PREFERENCE
    }
}
