package com.grippo.state.domain.goal

import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.data.features.api.goal.models.PersonalizationKeyEnum

public fun PersonalizationKeyEnumState.toDomain(): PersonalizationKeyEnum {
    return when (this) {
        PersonalizationKeyEnumState.LOW_FREQUENCY -> PersonalizationKeyEnum.LOW_FREQUENCY
        PersonalizationKeyEnumState.MODERATE_FREQUENCY -> PersonalizationKeyEnum.MODERATE_FREQUENCY
        PersonalizationKeyEnumState.HIGH_FREQUENCY -> PersonalizationKeyEnum.HIGH_FREQUENCY
        PersonalizationKeyEnumState.WEEKDAYS_ONLY -> PersonalizationKeyEnum.WEEKDAYS_ONLY
        PersonalizationKeyEnumState.WEEKENDS_ONLY -> PersonalizationKeyEnum.WEEKENDS_ONLY
        PersonalizationKeyEnumState.FLEXIBLE_SCHEDULE -> PersonalizationKeyEnum.FLEXIBLE_SCHEDULE
        PersonalizationKeyEnumState.UNPREDICTABLE_SCHEDULE -> PersonalizationKeyEnum.UNPREDICTABLE_SCHEDULE

        PersonalizationKeyEnumState.MINUTES_UP_TO_45 -> PersonalizationKeyEnum.MINUTES_UP_TO_45
        PersonalizationKeyEnumState.MINUTES_45_TO_90 -> PersonalizationKeyEnum.MINUTES_45_TO_90
        PersonalizationKeyEnumState.MINUTES_90_PLUS -> PersonalizationKeyEnum.MINUTES_90_PLUS
        PersonalizationKeyEnumState.FLEXIBLE_DURATION -> PersonalizationKeyEnum.FLEXIBLE_DURATION

        PersonalizationKeyEnumState.HIGH_STRESS_LIFESTYLE -> PersonalizationKeyEnum.HIGH_STRESS_LIFESTYLE
        PersonalizationKeyEnumState.POOR_SLEEP_PATTERN -> PersonalizationKeyEnum.POOR_SLEEP_PATTERN
        PersonalizationKeyEnumState.LOW_ENERGY_TENDENCY -> PersonalizationKeyEnum.LOW_ENERGY_TENDENCY
        PersonalizationKeyEnumState.SEDENTARY_JOB -> PersonalizationKeyEnum.SEDENTARY_JOB
        PersonalizationKeyEnumState.PHYSICALLY_ACTIVE_JOB -> PersonalizationKeyEnum.PHYSICALLY_ACTIVE_JOB

        PersonalizationKeyEnumState.RETURNING_AFTER_BREAK -> PersonalizationKeyEnum.RETURNING_AFTER_BREAK
        PersonalizationKeyEnumState.RECOVERING_FROM_INJURY -> PersonalizationKeyEnum.RECOVERING_FROM_INJURY
        PersonalizationKeyEnumState.LIMITED_RANGE_OF_MOTION -> PersonalizationKeyEnum.LIMITED_RANGE_OF_MOTION
        PersonalizationKeyEnumState.FATIGUE_MANAGEMENT -> PersonalizationKeyEnum.FATIGUE_MANAGEMENT
        PersonalizationKeyEnumState.RECOVERY_FOCUS -> PersonalizationKeyEnum.RECOVERY_FOCUS
        PersonalizationKeyEnumState.MOBILITY_FOCUS -> PersonalizationKeyEnum.MOBILITY_FOCUS

        PersonalizationKeyEnumState.NECK_CARE -> PersonalizationKeyEnum.NECK_CARE
        PersonalizationKeyEnumState.LOWER_BACK_CARE -> PersonalizationKeyEnum.LOWER_BACK_CARE
        PersonalizationKeyEnumState.UPPER_BACK_CARE -> PersonalizationKeyEnum.UPPER_BACK_CARE
        PersonalizationKeyEnumState.SHOULDER_CARE -> PersonalizationKeyEnum.SHOULDER_CARE
        PersonalizationKeyEnumState.ELBOW_CARE -> PersonalizationKeyEnum.ELBOW_CARE
        PersonalizationKeyEnumState.WRIST_CARE -> PersonalizationKeyEnum.WRIST_CARE
        PersonalizationKeyEnumState.HIP_CARE -> PersonalizationKeyEnum.HIP_CARE
        PersonalizationKeyEnumState.KNEE_CARE -> PersonalizationKeyEnum.KNEE_CARE
        PersonalizationKeyEnumState.ANKLE_CARE -> PersonalizationKeyEnum.ANKLE_CARE

        PersonalizationKeyEnumState.HYPERTENSION -> PersonalizationKeyEnum.HYPERTENSION
        PersonalizationKeyEnumState.DIABETES -> PersonalizationKeyEnum.DIABETES
        PersonalizationKeyEnumState.ASTHMA -> PersonalizationKeyEnum.ASTHMA
        PersonalizationKeyEnumState.OSTEOPOROSIS -> PersonalizationKeyEnum.OSTEOPOROSIS
        PersonalizationKeyEnumState.ARTHRITIS -> PersonalizationKeyEnum.ARTHRITIS
        PersonalizationKeyEnumState.HERNIATED_DISC -> PersonalizationKeyEnum.HERNIATED_DISC
        PersonalizationKeyEnumState.CHRONIC_PAIN -> PersonalizationKeyEnum.CHRONIC_PAIN
        PersonalizationKeyEnumState.PREGNANCY -> PersonalizationKeyEnum.PREGNANCY
        PersonalizationKeyEnumState.POSTPARTUM -> PersonalizationKeyEnum.POSTPARTUM

        PersonalizationKeyEnumState.AVOID_HIGH_INTENSITY -> PersonalizationKeyEnum.AVOID_HIGH_INTENSITY
        PersonalizationKeyEnumState.AVOID_HEAVY_LOADING -> PersonalizationKeyEnum.AVOID_HEAVY_LOADING
        PersonalizationKeyEnumState.AVOID_HIGH_IMPACT -> PersonalizationKeyEnum.AVOID_HIGH_IMPACT
        PersonalizationKeyEnumState.AVOID_DEEP_SQUATS -> PersonalizationKeyEnum.AVOID_DEEP_SQUATS
        PersonalizationKeyEnumState.AVOID_LUNGES -> PersonalizationKeyEnum.AVOID_LUNGES
        PersonalizationKeyEnumState.AVOID_OVERHEAD_WORK -> PersonalizationKeyEnum.AVOID_OVERHEAD_WORK
        PersonalizationKeyEnumState.AVOID_HEAVY_SPINAL_LOADING -> PersonalizationKeyEnum.AVOID_HEAVY_SPINAL_LOADING
        PersonalizationKeyEnumState.AVOID_SPINAL_FLEXION -> PersonalizationKeyEnum.AVOID_SPINAL_FLEXION
        PersonalizationKeyEnumState.AVOID_TWISTING -> PersonalizationKeyEnum.AVOID_TWISTING
        PersonalizationKeyEnumState.AVOID_FLOOR_WORK -> PersonalizationKeyEnum.AVOID_FLOOR_WORK
        PersonalizationKeyEnumState.AVOID_INVERSIONS -> PersonalizationKeyEnum.AVOID_INVERSIONS
        PersonalizationKeyEnumState.AVOID_VALSALVA -> PersonalizationKeyEnum.AVOID_VALSALVA
        PersonalizationKeyEnumState.NEEDS_SUPPORTED_MOVEMENTS -> PersonalizationKeyEnum.NEEDS_SUPPORTED_MOVEMENTS

        PersonalizationKeyEnumState.SPORT_POWERLIFTING -> PersonalizationKeyEnum.SPORT_POWERLIFTING
        PersonalizationKeyEnumState.SPORT_BODYBUILDING -> PersonalizationKeyEnum.SPORT_BODYBUILDING
        PersonalizationKeyEnumState.SPORT_OLYMPIC_WEIGHTLIFTING -> PersonalizationKeyEnum.SPORT_OLYMPIC_WEIGHTLIFTING
        PersonalizationKeyEnumState.SPORT_CROSSFIT -> PersonalizationKeyEnum.SPORT_CROSSFIT
        PersonalizationKeyEnumState.SPORT_CALISTHENICS -> PersonalizationKeyEnum.SPORT_CALISTHENICS
        PersonalizationKeyEnumState.SPORT_RUNNING -> PersonalizationKeyEnum.SPORT_RUNNING
        PersonalizationKeyEnumState.SPORT_CYCLING -> PersonalizationKeyEnum.SPORT_CYCLING
        PersonalizationKeyEnumState.SPORT_SWIMMING -> PersonalizationKeyEnum.SPORT_SWIMMING
        PersonalizationKeyEnumState.SPORT_GRAPPLING -> PersonalizationKeyEnum.SPORT_GRAPPLING
        PersonalizationKeyEnumState.SPORT_STRIKING -> PersonalizationKeyEnum.SPORT_STRIKING
        PersonalizationKeyEnumState.SPORT_TEAM_BALL_SPORTS -> PersonalizationKeyEnum.SPORT_TEAM_BALL_SPORTS
        PersonalizationKeyEnumState.SPORT_RACQUET_SPORTS -> PersonalizationKeyEnum.SPORT_RACQUET_SPORTS
        PersonalizationKeyEnumState.SPORT_CLIMBING -> PersonalizationKeyEnum.SPORT_CLIMBING
        PersonalizationKeyEnumState.SPORT_WINTER_BOARD_SPORTS -> PersonalizationKeyEnum.SPORT_WINTER_BOARD_SPORTS
        PersonalizationKeyEnumState.SPORT_YOGA_OR_PILATES -> PersonalizationKeyEnum.SPORT_YOGA_OR_PILATES

        PersonalizationKeyEnumState.FOCUS_CHEST -> PersonalizationKeyEnum.FOCUS_CHEST
        PersonalizationKeyEnumState.FOCUS_BACK -> PersonalizationKeyEnum.FOCUS_BACK
        PersonalizationKeyEnumState.FOCUS_SHOULDERS -> PersonalizationKeyEnum.FOCUS_SHOULDERS
        PersonalizationKeyEnumState.FOCUS_REAR_DELTS -> PersonalizationKeyEnum.FOCUS_REAR_DELTS
        PersonalizationKeyEnumState.FOCUS_ARMS -> PersonalizationKeyEnum.FOCUS_ARMS
        PersonalizationKeyEnumState.FOCUS_FOREARMS_AND_GRIP -> PersonalizationKeyEnum.FOCUS_FOREARMS_AND_GRIP
        PersonalizationKeyEnumState.FOCUS_ABS_AND_CORE -> PersonalizationKeyEnum.FOCUS_ABS_AND_CORE
        PersonalizationKeyEnumState.FOCUS_LEGS -> PersonalizationKeyEnum.FOCUS_LEGS
        PersonalizationKeyEnumState.FOCUS_GLUTES -> PersonalizationKeyEnum.FOCUS_GLUTES
        PersonalizationKeyEnumState.FOCUS_QUADRICEPS -> PersonalizationKeyEnum.FOCUS_QUADRICEPS
        PersonalizationKeyEnumState.FOCUS_HAMSTRINGS -> PersonalizationKeyEnum.FOCUS_HAMSTRINGS
        PersonalizationKeyEnumState.FOCUS_CALVES -> PersonalizationKeyEnum.FOCUS_CALVES

        PersonalizationKeyEnumState.IMPROVE_CARDIO -> PersonalizationKeyEnum.IMPROVE_CARDIO
        PersonalizationKeyEnumState.IMPROVE_MUSCULAR_ENDURANCE -> PersonalizationKeyEnum.IMPROVE_MUSCULAR_ENDURANCE
        PersonalizationKeyEnumState.IMPROVE_EXPLOSIVE_POWER -> PersonalizationKeyEnum.IMPROVE_EXPLOSIVE_POWER
        PersonalizationKeyEnumState.IMPROVE_SPEED -> PersonalizationKeyEnum.IMPROVE_SPEED
        PersonalizationKeyEnumState.IMPROVE_FLEXIBILITY -> PersonalizationKeyEnum.IMPROVE_FLEXIBILITY
        PersonalizationKeyEnumState.IMPROVE_BALANCE -> PersonalizationKeyEnum.IMPROVE_BALANCE
        PersonalizationKeyEnumState.IMPROVE_GRIP_STRENGTH -> PersonalizationKeyEnum.IMPROVE_GRIP_STRENGTH
        PersonalizationKeyEnumState.IMPROVE_CORE_STABILITY -> PersonalizationKeyEnum.IMPROVE_CORE_STABILITY

        PersonalizationKeyEnumState.SIMPLE_PLAN -> PersonalizationKeyEnum.SIMPLE_PLAN
        PersonalizationKeyEnumState.LOWER_VOLUME -> PersonalizationKeyEnum.LOWER_VOLUME
        PersonalizationKeyEnumState.HIGHER_VOLUME -> PersonalizationKeyEnum.HIGHER_VOLUME
        PersonalizationKeyEnumState.GRADUAL_PROGRESSION -> PersonalizationKeyEnum.GRADUAL_PROGRESSION
        PersonalizationKeyEnumState.AGGRESSIVE_PROGRESSION -> PersonalizationKeyEnum.AGGRESSIVE_PROGRESSION

        PersonalizationKeyEnumState.PREFER_FULL_BODY -> PersonalizationKeyEnum.PREFER_FULL_BODY
        PersonalizationKeyEnumState.PREFER_UPPER_LOWER -> PersonalizationKeyEnum.PREFER_UPPER_LOWER
        PersonalizationKeyEnumState.PREFER_PUSH_PULL_LEGS -> PersonalizationKeyEnum.PREFER_PUSH_PULL_LEGS
        PersonalizationKeyEnumState.PREFER_SPLIT_ROUTINE -> PersonalizationKeyEnum.PREFER_SPLIT_ROUTINE

        PersonalizationKeyEnumState.PREFER_STRAIGHT_SETS -> PersonalizationKeyEnum.PREFER_STRAIGHT_SETS
        PersonalizationKeyEnumState.PREFER_SUPERSETS -> PersonalizationKeyEnum.PREFER_SUPERSETS
        PersonalizationKeyEnumState.PREFER_CIRCUITS -> PersonalizationKeyEnum.PREFER_CIRCUITS
        PersonalizationKeyEnumState.PREFER_HEAVY_COMPOUNDS -> PersonalizationKeyEnum.PREFER_HEAVY_COMPOUNDS

        PersonalizationKeyEnumState.PREFER_FREE_WEIGHTS -> PersonalizationKeyEnum.PREFER_FREE_WEIGHTS
        PersonalizationKeyEnumState.PREFER_MACHINES -> PersonalizationKeyEnum.PREFER_MACHINES
        PersonalizationKeyEnumState.PREFER_CABLES -> PersonalizationKeyEnum.PREFER_CABLES
        PersonalizationKeyEnumState.PREFER_BODYWEIGHT -> PersonalizationKeyEnum.PREFER_BODYWEIGHT

        PersonalizationKeyEnumState.PHASE_BULKING -> PersonalizationKeyEnum.PHASE_BULKING
        PersonalizationKeyEnumState.PHASE_CUTTING -> PersonalizationKeyEnum.PHASE_CUTTING
        PersonalizationKeyEnumState.PHASE_MAINTENANCE -> PersonalizationKeyEnum.PHASE_MAINTENANCE
        PersonalizationKeyEnumState.PHASE_DELOAD -> PersonalizationKeyEnum.PHASE_DELOAD
        PersonalizationKeyEnumState.PHASE_COMPETITION_PREP -> PersonalizationKeyEnum.PHASE_COMPETITION_PREP
    }
}
