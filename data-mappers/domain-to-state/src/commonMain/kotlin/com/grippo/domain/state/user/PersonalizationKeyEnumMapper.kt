package com.grippo.domain.state.user

import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.data.features.api.goal.models.PersonalizationKeyEnum

public fun PersonalizationKeyEnum.toState(): PersonalizationKeyEnumState {
    return when (this) {
        PersonalizationKeyEnum.LOW_FREQUENCY -> PersonalizationKeyEnumState.LOW_FREQUENCY
        PersonalizationKeyEnum.MODERATE_FREQUENCY -> PersonalizationKeyEnumState.MODERATE_FREQUENCY
        PersonalizationKeyEnum.HIGH_FREQUENCY -> PersonalizationKeyEnumState.HIGH_FREQUENCY
        PersonalizationKeyEnum.WEEKDAYS_ONLY -> PersonalizationKeyEnumState.WEEKDAYS_ONLY
        PersonalizationKeyEnum.WEEKENDS_ONLY -> PersonalizationKeyEnumState.WEEKENDS_ONLY
        PersonalizationKeyEnum.FLEXIBLE_SCHEDULE -> PersonalizationKeyEnumState.FLEXIBLE_SCHEDULE
        PersonalizationKeyEnum.UNPREDICTABLE_SCHEDULE -> PersonalizationKeyEnumState.UNPREDICTABLE_SCHEDULE

        PersonalizationKeyEnum.MINUTES_UP_TO_45 -> PersonalizationKeyEnumState.MINUTES_UP_TO_45
        PersonalizationKeyEnum.MINUTES_45_TO_90 -> PersonalizationKeyEnumState.MINUTES_45_TO_90
        PersonalizationKeyEnum.MINUTES_90_PLUS -> PersonalizationKeyEnumState.MINUTES_90_PLUS
        PersonalizationKeyEnum.FLEXIBLE_DURATION -> PersonalizationKeyEnumState.FLEXIBLE_DURATION

        PersonalizationKeyEnum.HIGH_STRESS_LIFESTYLE -> PersonalizationKeyEnumState.HIGH_STRESS_LIFESTYLE
        PersonalizationKeyEnum.POOR_SLEEP_PATTERN -> PersonalizationKeyEnumState.POOR_SLEEP_PATTERN
        PersonalizationKeyEnum.LOW_ENERGY_TENDENCY -> PersonalizationKeyEnumState.LOW_ENERGY_TENDENCY
        PersonalizationKeyEnum.SEDENTARY_JOB -> PersonalizationKeyEnumState.SEDENTARY_JOB
        PersonalizationKeyEnum.PHYSICALLY_ACTIVE_JOB -> PersonalizationKeyEnumState.PHYSICALLY_ACTIVE_JOB

        PersonalizationKeyEnum.RETURNING_AFTER_BREAK -> PersonalizationKeyEnumState.RETURNING_AFTER_BREAK
        PersonalizationKeyEnum.RECOVERING_FROM_INJURY -> PersonalizationKeyEnumState.RECOVERING_FROM_INJURY
        PersonalizationKeyEnum.LIMITED_RANGE_OF_MOTION -> PersonalizationKeyEnumState.LIMITED_RANGE_OF_MOTION
        PersonalizationKeyEnum.FATIGUE_MANAGEMENT -> PersonalizationKeyEnumState.FATIGUE_MANAGEMENT
        PersonalizationKeyEnum.RECOVERY_FOCUS -> PersonalizationKeyEnumState.RECOVERY_FOCUS
        PersonalizationKeyEnum.MOBILITY_FOCUS -> PersonalizationKeyEnumState.MOBILITY_FOCUS

        PersonalizationKeyEnum.NECK_CARE -> PersonalizationKeyEnumState.NECK_CARE
        PersonalizationKeyEnum.LOWER_BACK_CARE -> PersonalizationKeyEnumState.LOWER_BACK_CARE
        PersonalizationKeyEnum.UPPER_BACK_CARE -> PersonalizationKeyEnumState.UPPER_BACK_CARE
        PersonalizationKeyEnum.SHOULDER_CARE -> PersonalizationKeyEnumState.SHOULDER_CARE
        PersonalizationKeyEnum.ELBOW_CARE -> PersonalizationKeyEnumState.ELBOW_CARE
        PersonalizationKeyEnum.WRIST_CARE -> PersonalizationKeyEnumState.WRIST_CARE
        PersonalizationKeyEnum.HIP_CARE -> PersonalizationKeyEnumState.HIP_CARE
        PersonalizationKeyEnum.KNEE_CARE -> PersonalizationKeyEnumState.KNEE_CARE
        PersonalizationKeyEnum.ANKLE_CARE -> PersonalizationKeyEnumState.ANKLE_CARE

        PersonalizationKeyEnum.HYPERTENSION -> PersonalizationKeyEnumState.HYPERTENSION
        PersonalizationKeyEnum.DIABETES -> PersonalizationKeyEnumState.DIABETES
        PersonalizationKeyEnum.ASTHMA -> PersonalizationKeyEnumState.ASTHMA
        PersonalizationKeyEnum.OSTEOPOROSIS -> PersonalizationKeyEnumState.OSTEOPOROSIS
        PersonalizationKeyEnum.ARTHRITIS -> PersonalizationKeyEnumState.ARTHRITIS
        PersonalizationKeyEnum.HERNIATED_DISC -> PersonalizationKeyEnumState.HERNIATED_DISC
        PersonalizationKeyEnum.CHRONIC_PAIN -> PersonalizationKeyEnumState.CHRONIC_PAIN
        PersonalizationKeyEnum.PREGNANCY -> PersonalizationKeyEnumState.PREGNANCY
        PersonalizationKeyEnum.POSTPARTUM -> PersonalizationKeyEnumState.POSTPARTUM

        PersonalizationKeyEnum.AVOID_HIGH_INTENSITY -> PersonalizationKeyEnumState.AVOID_HIGH_INTENSITY
        PersonalizationKeyEnum.AVOID_HEAVY_LOADING -> PersonalizationKeyEnumState.AVOID_HEAVY_LOADING
        PersonalizationKeyEnum.AVOID_HIGH_IMPACT -> PersonalizationKeyEnumState.AVOID_HIGH_IMPACT
        PersonalizationKeyEnum.AVOID_DEEP_SQUATS -> PersonalizationKeyEnumState.AVOID_DEEP_SQUATS
        PersonalizationKeyEnum.AVOID_LUNGES -> PersonalizationKeyEnumState.AVOID_LUNGES
        PersonalizationKeyEnum.AVOID_OVERHEAD_WORK -> PersonalizationKeyEnumState.AVOID_OVERHEAD_WORK
        PersonalizationKeyEnum.AVOID_HEAVY_SPINAL_LOADING -> PersonalizationKeyEnumState.AVOID_HEAVY_SPINAL_LOADING
        PersonalizationKeyEnum.AVOID_SPINAL_FLEXION -> PersonalizationKeyEnumState.AVOID_SPINAL_FLEXION
        PersonalizationKeyEnum.AVOID_TWISTING -> PersonalizationKeyEnumState.AVOID_TWISTING
        PersonalizationKeyEnum.AVOID_FLOOR_WORK -> PersonalizationKeyEnumState.AVOID_FLOOR_WORK
        PersonalizationKeyEnum.AVOID_INVERSIONS -> PersonalizationKeyEnumState.AVOID_INVERSIONS
        PersonalizationKeyEnum.AVOID_VALSALVA -> PersonalizationKeyEnumState.AVOID_VALSALVA
        PersonalizationKeyEnum.NEEDS_SUPPORTED_MOVEMENTS -> PersonalizationKeyEnumState.NEEDS_SUPPORTED_MOVEMENTS

        PersonalizationKeyEnum.SPORT_POWERLIFTING -> PersonalizationKeyEnumState.SPORT_POWERLIFTING
        PersonalizationKeyEnum.SPORT_BODYBUILDING -> PersonalizationKeyEnumState.SPORT_BODYBUILDING
        PersonalizationKeyEnum.SPORT_OLYMPIC_WEIGHTLIFTING -> PersonalizationKeyEnumState.SPORT_OLYMPIC_WEIGHTLIFTING
        PersonalizationKeyEnum.SPORT_CROSSFIT -> PersonalizationKeyEnumState.SPORT_CROSSFIT
        PersonalizationKeyEnum.SPORT_CALISTHENICS -> PersonalizationKeyEnumState.SPORT_CALISTHENICS
        PersonalizationKeyEnum.SPORT_RUNNING -> PersonalizationKeyEnumState.SPORT_RUNNING
        PersonalizationKeyEnum.SPORT_CYCLING -> PersonalizationKeyEnumState.SPORT_CYCLING
        PersonalizationKeyEnum.SPORT_SWIMMING -> PersonalizationKeyEnumState.SPORT_SWIMMING
        PersonalizationKeyEnum.SPORT_GRAPPLING -> PersonalizationKeyEnumState.SPORT_GRAPPLING
        PersonalizationKeyEnum.SPORT_STRIKING -> PersonalizationKeyEnumState.SPORT_STRIKING
        PersonalizationKeyEnum.SPORT_TEAM_BALL_SPORTS -> PersonalizationKeyEnumState.SPORT_TEAM_BALL_SPORTS
        PersonalizationKeyEnum.SPORT_RACQUET_SPORTS -> PersonalizationKeyEnumState.SPORT_RACQUET_SPORTS
        PersonalizationKeyEnum.SPORT_CLIMBING -> PersonalizationKeyEnumState.SPORT_CLIMBING
        PersonalizationKeyEnum.SPORT_WINTER_BOARD_SPORTS -> PersonalizationKeyEnumState.SPORT_WINTER_BOARD_SPORTS
        PersonalizationKeyEnum.SPORT_YOGA_OR_PILATES -> PersonalizationKeyEnumState.SPORT_YOGA_OR_PILATES

        PersonalizationKeyEnum.FOCUS_CHEST -> PersonalizationKeyEnumState.FOCUS_CHEST
        PersonalizationKeyEnum.FOCUS_BACK -> PersonalizationKeyEnumState.FOCUS_BACK
        PersonalizationKeyEnum.FOCUS_SHOULDERS -> PersonalizationKeyEnumState.FOCUS_SHOULDERS
        PersonalizationKeyEnum.FOCUS_REAR_DELTS -> PersonalizationKeyEnumState.FOCUS_REAR_DELTS
        PersonalizationKeyEnum.FOCUS_ARMS -> PersonalizationKeyEnumState.FOCUS_ARMS
        PersonalizationKeyEnum.FOCUS_FOREARMS_AND_GRIP -> PersonalizationKeyEnumState.FOCUS_FOREARMS_AND_GRIP
        PersonalizationKeyEnum.FOCUS_ABS_AND_CORE -> PersonalizationKeyEnumState.FOCUS_ABS_AND_CORE
        PersonalizationKeyEnum.FOCUS_LEGS -> PersonalizationKeyEnumState.FOCUS_LEGS
        PersonalizationKeyEnum.FOCUS_GLUTES -> PersonalizationKeyEnumState.FOCUS_GLUTES
        PersonalizationKeyEnum.FOCUS_QUADRICEPS -> PersonalizationKeyEnumState.FOCUS_QUADRICEPS
        PersonalizationKeyEnum.FOCUS_HAMSTRINGS -> PersonalizationKeyEnumState.FOCUS_HAMSTRINGS
        PersonalizationKeyEnum.FOCUS_CALVES -> PersonalizationKeyEnumState.FOCUS_CALVES

        PersonalizationKeyEnum.IMPROVE_CARDIO -> PersonalizationKeyEnumState.IMPROVE_CARDIO
        PersonalizationKeyEnum.IMPROVE_MUSCULAR_ENDURANCE -> PersonalizationKeyEnumState.IMPROVE_MUSCULAR_ENDURANCE
        PersonalizationKeyEnum.IMPROVE_EXPLOSIVE_POWER -> PersonalizationKeyEnumState.IMPROVE_EXPLOSIVE_POWER
        PersonalizationKeyEnum.IMPROVE_SPEED -> PersonalizationKeyEnumState.IMPROVE_SPEED
        PersonalizationKeyEnum.IMPROVE_FLEXIBILITY -> PersonalizationKeyEnumState.IMPROVE_FLEXIBILITY
        PersonalizationKeyEnum.IMPROVE_BALANCE -> PersonalizationKeyEnumState.IMPROVE_BALANCE
        PersonalizationKeyEnum.IMPROVE_GRIP_STRENGTH -> PersonalizationKeyEnumState.IMPROVE_GRIP_STRENGTH
        PersonalizationKeyEnum.IMPROVE_CORE_STABILITY -> PersonalizationKeyEnumState.IMPROVE_CORE_STABILITY

        PersonalizationKeyEnum.SIMPLE_PLAN -> PersonalizationKeyEnumState.SIMPLE_PLAN
        PersonalizationKeyEnum.LOWER_VOLUME -> PersonalizationKeyEnumState.LOWER_VOLUME
        PersonalizationKeyEnum.HIGHER_VOLUME -> PersonalizationKeyEnumState.HIGHER_VOLUME
        PersonalizationKeyEnum.GRADUAL_PROGRESSION -> PersonalizationKeyEnumState.GRADUAL_PROGRESSION
        PersonalizationKeyEnum.AGGRESSIVE_PROGRESSION -> PersonalizationKeyEnumState.AGGRESSIVE_PROGRESSION

        PersonalizationKeyEnum.PREFER_FULL_BODY -> PersonalizationKeyEnumState.PREFER_FULL_BODY
        PersonalizationKeyEnum.PREFER_UPPER_LOWER -> PersonalizationKeyEnumState.PREFER_UPPER_LOWER
        PersonalizationKeyEnum.PREFER_PUSH_PULL_LEGS -> PersonalizationKeyEnumState.PREFER_PUSH_PULL_LEGS
        PersonalizationKeyEnum.PREFER_SPLIT_ROUTINE -> PersonalizationKeyEnumState.PREFER_SPLIT_ROUTINE

        PersonalizationKeyEnum.PREFER_STRAIGHT_SETS -> PersonalizationKeyEnumState.PREFER_STRAIGHT_SETS
        PersonalizationKeyEnum.PREFER_SUPERSETS -> PersonalizationKeyEnumState.PREFER_SUPERSETS
        PersonalizationKeyEnum.PREFER_CIRCUITS -> PersonalizationKeyEnumState.PREFER_CIRCUITS
        PersonalizationKeyEnum.PREFER_HEAVY_COMPOUNDS -> PersonalizationKeyEnumState.PREFER_HEAVY_COMPOUNDS

        PersonalizationKeyEnum.PREFER_FREE_WEIGHTS -> PersonalizationKeyEnumState.PREFER_FREE_WEIGHTS
        PersonalizationKeyEnum.PREFER_MACHINES -> PersonalizationKeyEnumState.PREFER_MACHINES
        PersonalizationKeyEnum.PREFER_CABLES -> PersonalizationKeyEnumState.PREFER_CABLES
        PersonalizationKeyEnum.PREFER_BODYWEIGHT -> PersonalizationKeyEnumState.PREFER_BODYWEIGHT

        PersonalizationKeyEnum.PHASE_BULKING -> PersonalizationKeyEnumState.PHASE_BULKING
        PersonalizationKeyEnum.PHASE_CUTTING -> PersonalizationKeyEnumState.PHASE_CUTTING
        PersonalizationKeyEnum.PHASE_MAINTENANCE -> PersonalizationKeyEnumState.PHASE_MAINTENANCE
        PersonalizationKeyEnum.PHASE_DELOAD -> PersonalizationKeyEnumState.PHASE_DELOAD
        PersonalizationKeyEnum.PHASE_COMPETITION_PREP -> PersonalizationKeyEnumState.PHASE_COMPETITION_PREP
    }
}
