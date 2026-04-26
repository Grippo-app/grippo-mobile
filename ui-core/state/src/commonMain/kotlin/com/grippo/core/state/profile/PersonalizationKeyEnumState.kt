package com.grippo.core.state.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.personalization_aggressive_progression
import com.grippo.design.resources.provider.personalization_ankle_care
import com.grippo.design.resources.provider.personalization_arthritis
import com.grippo.design.resources.provider.personalization_asthma
import com.grippo.design.resources.provider.personalization_avoid_deep_squats
import com.grippo.design.resources.provider.personalization_avoid_floor_work
import com.grippo.design.resources.provider.personalization_avoid_heavy_loading
import com.grippo.design.resources.provider.personalization_avoid_heavy_spinal_loading
import com.grippo.design.resources.provider.personalization_avoid_high_impact
import com.grippo.design.resources.provider.personalization_avoid_high_intensity
import com.grippo.design.resources.provider.personalization_avoid_inversions
import com.grippo.design.resources.provider.personalization_avoid_lunges
import com.grippo.design.resources.provider.personalization_avoid_overhead_work
import com.grippo.design.resources.provider.personalization_avoid_spinal_flexion
import com.grippo.design.resources.provider.personalization_avoid_twisting
import com.grippo.design.resources.provider.personalization_avoid_valsalva
import com.grippo.design.resources.provider.personalization_chronic_pain
import com.grippo.design.resources.provider.personalization_diabetes
import com.grippo.design.resources.provider.personalization_elbow_care
import com.grippo.design.resources.provider.personalization_fatigue_management
import com.grippo.design.resources.provider.personalization_flexible_duration
import com.grippo.design.resources.provider.personalization_flexible_schedule
import com.grippo.design.resources.provider.personalization_focus_abs_and_core
import com.grippo.design.resources.provider.personalization_focus_arms
import com.grippo.design.resources.provider.personalization_focus_back
import com.grippo.design.resources.provider.personalization_focus_calves
import com.grippo.design.resources.provider.personalization_focus_chest
import com.grippo.design.resources.provider.personalization_focus_forearms_and_grip
import com.grippo.design.resources.provider.personalization_focus_glutes
import com.grippo.design.resources.provider.personalization_focus_hamstrings
import com.grippo.design.resources.provider.personalization_focus_legs
import com.grippo.design.resources.provider.personalization_focus_quadriceps
import com.grippo.design.resources.provider.personalization_focus_rear_delts
import com.grippo.design.resources.provider.personalization_focus_shoulders
import com.grippo.design.resources.provider.personalization_gradual_progression
import com.grippo.design.resources.provider.personalization_herniated_disc
import com.grippo.design.resources.provider.personalization_high_frequency
import com.grippo.design.resources.provider.personalization_high_stress_lifestyle
import com.grippo.design.resources.provider.personalization_higher_volume
import com.grippo.design.resources.provider.personalization_hip_care
import com.grippo.design.resources.provider.personalization_hypertension
import com.grippo.design.resources.provider.personalization_improve_balance
import com.grippo.design.resources.provider.personalization_improve_cardio
import com.grippo.design.resources.provider.personalization_improve_core_stability
import com.grippo.design.resources.provider.personalization_improve_explosive_power
import com.grippo.design.resources.provider.personalization_improve_flexibility
import com.grippo.design.resources.provider.personalization_improve_grip_strength
import com.grippo.design.resources.provider.personalization_improve_muscular_endurance
import com.grippo.design.resources.provider.personalization_improve_speed
import com.grippo.design.resources.provider.personalization_knee_care
import com.grippo.design.resources.provider.personalization_limited_range_of_motion
import com.grippo.design.resources.provider.personalization_low_energy_tendency
import com.grippo.design.resources.provider.personalization_low_frequency
import com.grippo.design.resources.provider.personalization_lower_back_care
import com.grippo.design.resources.provider.personalization_lower_volume
import com.grippo.design.resources.provider.personalization_minutes_45_to_90
import com.grippo.design.resources.provider.personalization_minutes_90_plus
import com.grippo.design.resources.provider.personalization_minutes_up_to_45
import com.grippo.design.resources.provider.personalization_mobility_focus
import com.grippo.design.resources.provider.personalization_moderate_frequency
import com.grippo.design.resources.provider.personalization_neck_care
import com.grippo.design.resources.provider.personalization_needs_supported_movements
import com.grippo.design.resources.provider.personalization_osteoporosis
import com.grippo.design.resources.provider.personalization_phase_bulking
import com.grippo.design.resources.provider.personalization_phase_competition_prep
import com.grippo.design.resources.provider.personalization_phase_cutting
import com.grippo.design.resources.provider.personalization_phase_deload
import com.grippo.design.resources.provider.personalization_phase_maintenance
import com.grippo.design.resources.provider.personalization_physically_active_job
import com.grippo.design.resources.provider.personalization_poor_sleep_pattern
import com.grippo.design.resources.provider.personalization_postpartum
import com.grippo.design.resources.provider.personalization_prefer_bodyweight
import com.grippo.design.resources.provider.personalization_prefer_cables
import com.grippo.design.resources.provider.personalization_prefer_circuits
import com.grippo.design.resources.provider.personalization_prefer_free_weights
import com.grippo.design.resources.provider.personalization_prefer_full_body
import com.grippo.design.resources.provider.personalization_prefer_heavy_compounds
import com.grippo.design.resources.provider.personalization_prefer_machines
import com.grippo.design.resources.provider.personalization_prefer_push_pull_legs
import com.grippo.design.resources.provider.personalization_prefer_split_routine
import com.grippo.design.resources.provider.personalization_prefer_straight_sets
import com.grippo.design.resources.provider.personalization_prefer_supersets
import com.grippo.design.resources.provider.personalization_prefer_upper_lower
import com.grippo.design.resources.provider.personalization_pregnancy
import com.grippo.design.resources.provider.personalization_recovering_from_injury
import com.grippo.design.resources.provider.personalization_recovery_focus
import com.grippo.design.resources.provider.personalization_returning_after_break
import com.grippo.design.resources.provider.personalization_sedentary_job
import com.grippo.design.resources.provider.personalization_shoulder_care
import com.grippo.design.resources.provider.personalization_simple_plan
import com.grippo.design.resources.provider.personalization_sport_bodybuilding
import com.grippo.design.resources.provider.personalization_sport_calisthenics
import com.grippo.design.resources.provider.personalization_sport_climbing
import com.grippo.design.resources.provider.personalization_sport_crossfit
import com.grippo.design.resources.provider.personalization_sport_cycling
import com.grippo.design.resources.provider.personalization_sport_grappling
import com.grippo.design.resources.provider.personalization_sport_olympic_weightlifting
import com.grippo.design.resources.provider.personalization_sport_powerlifting
import com.grippo.design.resources.provider.personalization_sport_racquet_sports
import com.grippo.design.resources.provider.personalization_sport_running
import com.grippo.design.resources.provider.personalization_sport_striking
import com.grippo.design.resources.provider.personalization_sport_swimming
import com.grippo.design.resources.provider.personalization_sport_team_ball_sports
import com.grippo.design.resources.provider.personalization_sport_winter_board_sports
import com.grippo.design.resources.provider.personalization_sport_yoga_or_pilates
import com.grippo.design.resources.provider.personalization_unpredictable_schedule
import com.grippo.design.resources.provider.personalization_upper_back_care
import com.grippo.design.resources.provider.personalization_weekdays_only
import com.grippo.design.resources.provider.personalization_weekends_only
import com.grippo.design.resources.provider.personalization_wrist_care
import com.grippo.design.resources.provider.personalizations_care_zones_section
import com.grippo.design.resources.provider.personalizations_equipment_preferences_section
import com.grippo.design.resources.provider.personalizations_focus_areas_section
import com.grippo.design.resources.provider.personalizations_health_conditions_section
import com.grippo.design.resources.provider.personalizations_lifestyle_section
import com.grippo.design.resources.provider.personalizations_movement_restrictions_section
import com.grippo.design.resources.provider.personalizations_physical_qualities_section
import com.grippo.design.resources.provider.personalizations_program_preferences_section
import com.grippo.design.resources.provider.personalizations_recovery_context_section
import com.grippo.design.resources.provider.personalizations_routine_structure_section
import com.grippo.design.resources.provider.personalizations_schedule_section
import com.grippo.design.resources.provider.personalizations_session_duration_section
import com.grippo.design.resources.provider.personalizations_set_method_preferences_section
import com.grippo.design.resources.provider.personalizations_sports_section
import com.grippo.design.resources.provider.personalizations_training_phase_section

@Immutable
public enum class PersonalizationKeyEnumState {
    // 1. Schedule
    LOW_FREQUENCY,
    MODERATE_FREQUENCY,
    HIGH_FREQUENCY,
    WEEKDAYS_ONLY,
    WEEKENDS_ONLY,
    FLEXIBLE_SCHEDULE,
    UNPREDICTABLE_SCHEDULE,

    // 2. Session duration
    MINUTES_UP_TO_45,
    MINUTES_45_TO_90,
    MINUTES_90_PLUS,
    FLEXIBLE_DURATION,

    // 3. Lifestyle
    HIGH_STRESS_LIFESTYLE,
    POOR_SLEEP_PATTERN,
    LOW_ENERGY_TENDENCY,
    SEDENTARY_JOB,
    PHYSICALLY_ACTIVE_JOB,

    // 4. Recovery context
    RETURNING_AFTER_BREAK,
    RECOVERING_FROM_INJURY,
    LIMITED_RANGE_OF_MOTION,
    FATIGUE_MANAGEMENT,
    RECOVERY_FOCUS,
    MOBILITY_FOCUS,

    // 5. Care zones
    NECK_CARE,
    LOWER_BACK_CARE,
    UPPER_BACK_CARE,
    SHOULDER_CARE,
    ELBOW_CARE,
    WRIST_CARE,
    HIP_CARE,
    KNEE_CARE,
    ANKLE_CARE,

    // 6. Health conditions
    HYPERTENSION,
    DIABETES,
    ASTHMA,
    OSTEOPOROSIS,
    ARTHRITIS,
    HERNIATED_DISC,
    CHRONIC_PAIN,
    PREGNANCY,
    POSTPARTUM,

    // 7. Movement restrictions
    AVOID_HIGH_INTENSITY,
    AVOID_HEAVY_LOADING,
    AVOID_HIGH_IMPACT,
    AVOID_DEEP_SQUATS,
    AVOID_LUNGES,
    AVOID_OVERHEAD_WORK,
    AVOID_HEAVY_SPINAL_LOADING,
    AVOID_SPINAL_FLEXION,
    AVOID_TWISTING,
    AVOID_FLOOR_WORK,
    AVOID_INVERSIONS,
    AVOID_VALSALVA,
    NEEDS_SUPPORTED_MOVEMENTS,

    // 8. Sports
    SPORT_POWERLIFTING,
    SPORT_BODYBUILDING,
    SPORT_OLYMPIC_WEIGHTLIFTING,
    SPORT_CROSSFIT,
    SPORT_CALISTHENICS,
    SPORT_RUNNING,
    SPORT_CYCLING,
    SPORT_SWIMMING,
    SPORT_GRAPPLING,
    SPORT_STRIKING,
    SPORT_TEAM_BALL_SPORTS,
    SPORT_RACQUET_SPORTS,
    SPORT_CLIMBING,
    SPORT_WINTER_BOARD_SPORTS,
    SPORT_YOGA_OR_PILATES,

    // 9. Focus areas
    FOCUS_CHEST,
    FOCUS_BACK,
    FOCUS_SHOULDERS,
    FOCUS_REAR_DELTS,
    FOCUS_ARMS,
    FOCUS_FOREARMS_AND_GRIP,
    FOCUS_ABS_AND_CORE,
    FOCUS_LEGS,
    FOCUS_GLUTES,
    FOCUS_QUADRICEPS,
    FOCUS_HAMSTRINGS,
    FOCUS_CALVES,

    // 10. Physical qualities
    IMPROVE_CARDIO,
    IMPROVE_MUSCULAR_ENDURANCE,
    IMPROVE_EXPLOSIVE_POWER,
    IMPROVE_SPEED,
    IMPROVE_FLEXIBILITY,
    IMPROVE_BALANCE,
    IMPROVE_GRIP_STRENGTH,
    IMPROVE_CORE_STABILITY,

    // 11. Program preferences
    SIMPLE_PLAN,
    LOWER_VOLUME,
    HIGHER_VOLUME,
    GRADUAL_PROGRESSION,
    AGGRESSIVE_PROGRESSION,

    // 12. Routine structure
    PREFER_FULL_BODY,
    PREFER_UPPER_LOWER,
    PREFER_PUSH_PULL_LEGS,
    PREFER_SPLIT_ROUTINE,

    // 13. Set / method preferences
    PREFER_STRAIGHT_SETS,
    PREFER_SUPERSETS,
    PREFER_CIRCUITS,
    PREFER_HEAVY_COMPOUNDS,

    // 14. Equipment preferences
    PREFER_FREE_WEIGHTS,
    PREFER_MACHINES,
    PREFER_CABLES,
    PREFER_BODYWEIGHT,

    // 15. Training phase
    PHASE_BULKING,
    PHASE_CUTTING,
    PHASE_MAINTENANCE,
    PHASE_DELOAD,
    PHASE_COMPETITION_PREP;

    /**
     * Logical bucket the entry belongs to.
     *
     * Drives section grouping in screens that surface the full personalization
     * list, so the user sees coherent clusters instead of one flat wall of chips.
     */
    public fun category(): Category = when (this) {
        LOW_FREQUENCY,
        MODERATE_FREQUENCY,
        HIGH_FREQUENCY,
        WEEKDAYS_ONLY,
        WEEKENDS_ONLY,
        FLEXIBLE_SCHEDULE,
        UNPREDICTABLE_SCHEDULE -> Category.SCHEDULE

        MINUTES_UP_TO_45,
        MINUTES_45_TO_90,
        MINUTES_90_PLUS,
        FLEXIBLE_DURATION -> Category.SESSION_DURATION

        HIGH_STRESS_LIFESTYLE,
        POOR_SLEEP_PATTERN,
        LOW_ENERGY_TENDENCY,
        SEDENTARY_JOB,
        PHYSICALLY_ACTIVE_JOB -> Category.LIFESTYLE

        RETURNING_AFTER_BREAK,
        RECOVERING_FROM_INJURY,
        LIMITED_RANGE_OF_MOTION,
        FATIGUE_MANAGEMENT,
        RECOVERY_FOCUS,
        MOBILITY_FOCUS -> Category.RECOVERY_CONTEXT

        NECK_CARE,
        LOWER_BACK_CARE,
        UPPER_BACK_CARE,
        SHOULDER_CARE,
        ELBOW_CARE,
        WRIST_CARE,
        HIP_CARE,
        KNEE_CARE,
        ANKLE_CARE -> Category.CARE_ZONES

        HYPERTENSION,
        DIABETES,
        ASTHMA,
        OSTEOPOROSIS,
        ARTHRITIS,
        HERNIATED_DISC,
        CHRONIC_PAIN,
        PREGNANCY,
        POSTPARTUM -> Category.HEALTH_CONDITIONS

        AVOID_HIGH_INTENSITY,
        AVOID_HEAVY_LOADING,
        AVOID_HIGH_IMPACT,
        AVOID_DEEP_SQUATS,
        AVOID_LUNGES,
        AVOID_OVERHEAD_WORK,
        AVOID_HEAVY_SPINAL_LOADING,
        AVOID_SPINAL_FLEXION,
        AVOID_TWISTING,
        AVOID_FLOOR_WORK,
        AVOID_INVERSIONS,
        AVOID_VALSALVA,
        NEEDS_SUPPORTED_MOVEMENTS -> Category.MOVEMENT_RESTRICTIONS

        SPORT_POWERLIFTING,
        SPORT_BODYBUILDING,
        SPORT_OLYMPIC_WEIGHTLIFTING,
        SPORT_CROSSFIT,
        SPORT_CALISTHENICS,
        SPORT_RUNNING,
        SPORT_CYCLING,
        SPORT_SWIMMING,
        SPORT_GRAPPLING,
        SPORT_STRIKING,
        SPORT_TEAM_BALL_SPORTS,
        SPORT_RACQUET_SPORTS,
        SPORT_CLIMBING,
        SPORT_WINTER_BOARD_SPORTS,
        SPORT_YOGA_OR_PILATES -> Category.SPORTS

        FOCUS_CHEST,
        FOCUS_BACK,
        FOCUS_SHOULDERS,
        FOCUS_REAR_DELTS,
        FOCUS_ARMS,
        FOCUS_FOREARMS_AND_GRIP,
        FOCUS_ABS_AND_CORE,
        FOCUS_LEGS,
        FOCUS_GLUTES,
        FOCUS_QUADRICEPS,
        FOCUS_HAMSTRINGS,
        FOCUS_CALVES -> Category.FOCUS_AREAS

        IMPROVE_CARDIO,
        IMPROVE_MUSCULAR_ENDURANCE,
        IMPROVE_EXPLOSIVE_POWER,
        IMPROVE_SPEED,
        IMPROVE_FLEXIBILITY,
        IMPROVE_BALANCE,
        IMPROVE_GRIP_STRENGTH,
        IMPROVE_CORE_STABILITY -> Category.PHYSICAL_QUALITIES

        SIMPLE_PLAN,
        LOWER_VOLUME,
        HIGHER_VOLUME,
        GRADUAL_PROGRESSION,
        AGGRESSIVE_PROGRESSION -> Category.PROGRAM_PREFERENCES

        PREFER_FULL_BODY,
        PREFER_UPPER_LOWER,
        PREFER_PUSH_PULL_LEGS,
        PREFER_SPLIT_ROUTINE -> Category.ROUTINE_STRUCTURE

        PREFER_STRAIGHT_SETS,
        PREFER_SUPERSETS,
        PREFER_CIRCUITS,
        PREFER_HEAVY_COMPOUNDS -> Category.SET_METHOD_PREFERENCES

        PREFER_FREE_WEIGHTS,
        PREFER_MACHINES,
        PREFER_CABLES,
        PREFER_BODYWEIGHT -> Category.EQUIPMENT_PREFERENCES

        PHASE_BULKING,
        PHASE_CUTTING,
        PHASE_MAINTENANCE,
        PHASE_DELOAD,
        PHASE_COMPETITION_PREP -> Category.TRAINING_PHASE
    }

    @Composable
    public fun label(): String {
        return when (this) {
            LOW_FREQUENCY -> AppTokens.strings.res(Res.string.personalization_low_frequency)
            MODERATE_FREQUENCY -> AppTokens.strings.res(Res.string.personalization_moderate_frequency)
            HIGH_FREQUENCY -> AppTokens.strings.res(Res.string.personalization_high_frequency)
            WEEKDAYS_ONLY -> AppTokens.strings.res(Res.string.personalization_weekdays_only)
            WEEKENDS_ONLY -> AppTokens.strings.res(Res.string.personalization_weekends_only)
            FLEXIBLE_SCHEDULE -> AppTokens.strings.res(Res.string.personalization_flexible_schedule)
            UNPREDICTABLE_SCHEDULE -> AppTokens.strings.res(Res.string.personalization_unpredictable_schedule)

            MINUTES_UP_TO_45 -> AppTokens.strings.res(Res.string.personalization_minutes_up_to_45)
            MINUTES_45_TO_90 -> AppTokens.strings.res(Res.string.personalization_minutes_45_to_90)
            MINUTES_90_PLUS -> AppTokens.strings.res(Res.string.personalization_minutes_90_plus)
            FLEXIBLE_DURATION -> AppTokens.strings.res(Res.string.personalization_flexible_duration)

            HIGH_STRESS_LIFESTYLE -> AppTokens.strings.res(Res.string.personalization_high_stress_lifestyle)
            POOR_SLEEP_PATTERN -> AppTokens.strings.res(Res.string.personalization_poor_sleep_pattern)
            LOW_ENERGY_TENDENCY -> AppTokens.strings.res(Res.string.personalization_low_energy_tendency)
            SEDENTARY_JOB -> AppTokens.strings.res(Res.string.personalization_sedentary_job)
            PHYSICALLY_ACTIVE_JOB -> AppTokens.strings.res(Res.string.personalization_physically_active_job)

            RETURNING_AFTER_BREAK -> AppTokens.strings.res(Res.string.personalization_returning_after_break)
            RECOVERING_FROM_INJURY -> AppTokens.strings.res(Res.string.personalization_recovering_from_injury)
            LIMITED_RANGE_OF_MOTION -> AppTokens.strings.res(Res.string.personalization_limited_range_of_motion)
            FATIGUE_MANAGEMENT -> AppTokens.strings.res(Res.string.personalization_fatigue_management)
            RECOVERY_FOCUS -> AppTokens.strings.res(Res.string.personalization_recovery_focus)
            MOBILITY_FOCUS -> AppTokens.strings.res(Res.string.personalization_mobility_focus)

            NECK_CARE -> AppTokens.strings.res(Res.string.personalization_neck_care)
            LOWER_BACK_CARE -> AppTokens.strings.res(Res.string.personalization_lower_back_care)
            UPPER_BACK_CARE -> AppTokens.strings.res(Res.string.personalization_upper_back_care)
            SHOULDER_CARE -> AppTokens.strings.res(Res.string.personalization_shoulder_care)
            ELBOW_CARE -> AppTokens.strings.res(Res.string.personalization_elbow_care)
            WRIST_CARE -> AppTokens.strings.res(Res.string.personalization_wrist_care)
            HIP_CARE -> AppTokens.strings.res(Res.string.personalization_hip_care)
            KNEE_CARE -> AppTokens.strings.res(Res.string.personalization_knee_care)
            ANKLE_CARE -> AppTokens.strings.res(Res.string.personalization_ankle_care)

            HYPERTENSION -> AppTokens.strings.res(Res.string.personalization_hypertension)
            DIABETES -> AppTokens.strings.res(Res.string.personalization_diabetes)
            ASTHMA -> AppTokens.strings.res(Res.string.personalization_asthma)
            OSTEOPOROSIS -> AppTokens.strings.res(Res.string.personalization_osteoporosis)
            ARTHRITIS -> AppTokens.strings.res(Res.string.personalization_arthritis)
            HERNIATED_DISC -> AppTokens.strings.res(Res.string.personalization_herniated_disc)
            CHRONIC_PAIN -> AppTokens.strings.res(Res.string.personalization_chronic_pain)
            PREGNANCY -> AppTokens.strings.res(Res.string.personalization_pregnancy)
            POSTPARTUM -> AppTokens.strings.res(Res.string.personalization_postpartum)

            AVOID_HIGH_INTENSITY -> AppTokens.strings.res(Res.string.personalization_avoid_high_intensity)
            AVOID_HEAVY_LOADING -> AppTokens.strings.res(Res.string.personalization_avoid_heavy_loading)
            AVOID_HIGH_IMPACT -> AppTokens.strings.res(Res.string.personalization_avoid_high_impact)
            AVOID_DEEP_SQUATS -> AppTokens.strings.res(Res.string.personalization_avoid_deep_squats)
            AVOID_LUNGES -> AppTokens.strings.res(Res.string.personalization_avoid_lunges)
            AVOID_OVERHEAD_WORK -> AppTokens.strings.res(Res.string.personalization_avoid_overhead_work)
            AVOID_HEAVY_SPINAL_LOADING -> AppTokens.strings.res(Res.string.personalization_avoid_heavy_spinal_loading)
            AVOID_SPINAL_FLEXION -> AppTokens.strings.res(Res.string.personalization_avoid_spinal_flexion)
            AVOID_TWISTING -> AppTokens.strings.res(Res.string.personalization_avoid_twisting)
            AVOID_FLOOR_WORK -> AppTokens.strings.res(Res.string.personalization_avoid_floor_work)
            AVOID_INVERSIONS -> AppTokens.strings.res(Res.string.personalization_avoid_inversions)
            AVOID_VALSALVA -> AppTokens.strings.res(Res.string.personalization_avoid_valsalva)
            NEEDS_SUPPORTED_MOVEMENTS -> AppTokens.strings.res(Res.string.personalization_needs_supported_movements)

            SPORT_POWERLIFTING -> AppTokens.strings.res(Res.string.personalization_sport_powerlifting)
            SPORT_BODYBUILDING -> AppTokens.strings.res(Res.string.personalization_sport_bodybuilding)
            SPORT_OLYMPIC_WEIGHTLIFTING -> AppTokens.strings.res(Res.string.personalization_sport_olympic_weightlifting)
            SPORT_CROSSFIT -> AppTokens.strings.res(Res.string.personalization_sport_crossfit)
            SPORT_CALISTHENICS -> AppTokens.strings.res(Res.string.personalization_sport_calisthenics)
            SPORT_RUNNING -> AppTokens.strings.res(Res.string.personalization_sport_running)
            SPORT_CYCLING -> AppTokens.strings.res(Res.string.personalization_sport_cycling)
            SPORT_SWIMMING -> AppTokens.strings.res(Res.string.personalization_sport_swimming)
            SPORT_GRAPPLING -> AppTokens.strings.res(Res.string.personalization_sport_grappling)
            SPORT_STRIKING -> AppTokens.strings.res(Res.string.personalization_sport_striking)
            SPORT_TEAM_BALL_SPORTS -> AppTokens.strings.res(Res.string.personalization_sport_team_ball_sports)
            SPORT_RACQUET_SPORTS -> AppTokens.strings.res(Res.string.personalization_sport_racquet_sports)
            SPORT_CLIMBING -> AppTokens.strings.res(Res.string.personalization_sport_climbing)
            SPORT_WINTER_BOARD_SPORTS -> AppTokens.strings.res(Res.string.personalization_sport_winter_board_sports)
            SPORT_YOGA_OR_PILATES -> AppTokens.strings.res(Res.string.personalization_sport_yoga_or_pilates)

            FOCUS_CHEST -> AppTokens.strings.res(Res.string.personalization_focus_chest)
            FOCUS_BACK -> AppTokens.strings.res(Res.string.personalization_focus_back)
            FOCUS_SHOULDERS -> AppTokens.strings.res(Res.string.personalization_focus_shoulders)
            FOCUS_REAR_DELTS -> AppTokens.strings.res(Res.string.personalization_focus_rear_delts)
            FOCUS_ARMS -> AppTokens.strings.res(Res.string.personalization_focus_arms)
            FOCUS_FOREARMS_AND_GRIP -> AppTokens.strings.res(Res.string.personalization_focus_forearms_and_grip)
            FOCUS_ABS_AND_CORE -> AppTokens.strings.res(Res.string.personalization_focus_abs_and_core)
            FOCUS_LEGS -> AppTokens.strings.res(Res.string.personalization_focus_legs)
            FOCUS_GLUTES -> AppTokens.strings.res(Res.string.personalization_focus_glutes)
            FOCUS_QUADRICEPS -> AppTokens.strings.res(Res.string.personalization_focus_quadriceps)
            FOCUS_HAMSTRINGS -> AppTokens.strings.res(Res.string.personalization_focus_hamstrings)
            FOCUS_CALVES -> AppTokens.strings.res(Res.string.personalization_focus_calves)

            IMPROVE_CARDIO -> AppTokens.strings.res(Res.string.personalization_improve_cardio)
            IMPROVE_MUSCULAR_ENDURANCE -> AppTokens.strings.res(Res.string.personalization_improve_muscular_endurance)
            IMPROVE_EXPLOSIVE_POWER -> AppTokens.strings.res(Res.string.personalization_improve_explosive_power)
            IMPROVE_SPEED -> AppTokens.strings.res(Res.string.personalization_improve_speed)
            IMPROVE_FLEXIBILITY -> AppTokens.strings.res(Res.string.personalization_improve_flexibility)
            IMPROVE_BALANCE -> AppTokens.strings.res(Res.string.personalization_improve_balance)
            IMPROVE_GRIP_STRENGTH -> AppTokens.strings.res(Res.string.personalization_improve_grip_strength)
            IMPROVE_CORE_STABILITY -> AppTokens.strings.res(Res.string.personalization_improve_core_stability)

            SIMPLE_PLAN -> AppTokens.strings.res(Res.string.personalization_simple_plan)
            LOWER_VOLUME -> AppTokens.strings.res(Res.string.personalization_lower_volume)
            HIGHER_VOLUME -> AppTokens.strings.res(Res.string.personalization_higher_volume)
            GRADUAL_PROGRESSION -> AppTokens.strings.res(Res.string.personalization_gradual_progression)
            AGGRESSIVE_PROGRESSION -> AppTokens.strings.res(Res.string.personalization_aggressive_progression)

            PREFER_FULL_BODY -> AppTokens.strings.res(Res.string.personalization_prefer_full_body)
            PREFER_UPPER_LOWER -> AppTokens.strings.res(Res.string.personalization_prefer_upper_lower)
            PREFER_PUSH_PULL_LEGS -> AppTokens.strings.res(Res.string.personalization_prefer_push_pull_legs)
            PREFER_SPLIT_ROUTINE -> AppTokens.strings.res(Res.string.personalization_prefer_split_routine)

            PREFER_STRAIGHT_SETS -> AppTokens.strings.res(Res.string.personalization_prefer_straight_sets)
            PREFER_SUPERSETS -> AppTokens.strings.res(Res.string.personalization_prefer_supersets)
            PREFER_CIRCUITS -> AppTokens.strings.res(Res.string.personalization_prefer_circuits)
            PREFER_HEAVY_COMPOUNDS -> AppTokens.strings.res(Res.string.personalization_prefer_heavy_compounds)

            PREFER_FREE_WEIGHTS -> AppTokens.strings.res(Res.string.personalization_prefer_free_weights)
            PREFER_MACHINES -> AppTokens.strings.res(Res.string.personalization_prefer_machines)
            PREFER_CABLES -> AppTokens.strings.res(Res.string.personalization_prefer_cables)
            PREFER_BODYWEIGHT -> AppTokens.strings.res(Res.string.personalization_prefer_bodyweight)

            PHASE_BULKING -> AppTokens.strings.res(Res.string.personalization_phase_bulking)
            PHASE_CUTTING -> AppTokens.strings.res(Res.string.personalization_phase_cutting)
            PHASE_MAINTENANCE -> AppTokens.strings.res(Res.string.personalization_phase_maintenance)
            PHASE_DELOAD -> AppTokens.strings.res(Res.string.personalization_phase_deload)
            PHASE_COMPETITION_PREP -> AppTokens.strings.res(Res.string.personalization_phase_competition_prep)
        }
    }

    @Immutable
    public enum class Category {
        SCHEDULE,
        SESSION_DURATION,
        LIFESTYLE,
        RECOVERY_CONTEXT,
        CARE_ZONES,
        HEALTH_CONDITIONS,
        MOVEMENT_RESTRICTIONS,
        SPORTS,
        FOCUS_AREAS,
        PHYSICAL_QUALITIES,
        PROGRAM_PREFERENCES,
        ROUTINE_STRUCTURE,
        SET_METHOD_PREFERENCES,
        EQUIPMENT_PREFERENCES,
        TRAINING_PHASE;

        @Composable
        public fun label(): String = when (this) {
            SCHEDULE -> AppTokens.strings.res(Res.string.personalizations_schedule_section)
            SESSION_DURATION -> AppTokens.strings.res(Res.string.personalizations_session_duration_section)
            LIFESTYLE -> AppTokens.strings.res(Res.string.personalizations_lifestyle_section)
            RECOVERY_CONTEXT -> AppTokens.strings.res(Res.string.personalizations_recovery_context_section)
            CARE_ZONES -> AppTokens.strings.res(Res.string.personalizations_care_zones_section)
            HEALTH_CONDITIONS -> AppTokens.strings.res(Res.string.personalizations_health_conditions_section)
            MOVEMENT_RESTRICTIONS -> AppTokens.strings.res(Res.string.personalizations_movement_restrictions_section)
            SPORTS -> AppTokens.strings.res(Res.string.personalizations_sports_section)
            FOCUS_AREAS -> AppTokens.strings.res(Res.string.personalizations_focus_areas_section)
            PHYSICAL_QUALITIES -> AppTokens.strings.res(Res.string.personalizations_physical_qualities_section)
            PROGRAM_PREFERENCES -> AppTokens.strings.res(Res.string.personalizations_program_preferences_section)
            ROUTINE_STRUCTURE -> AppTokens.strings.res(Res.string.personalizations_routine_structure_section)
            SET_METHOD_PREFERENCES -> AppTokens.strings.res(Res.string.personalizations_set_method_preferences_section)
            EQUIPMENT_PREFERENCES -> AppTokens.strings.res(Res.string.personalizations_equipment_preferences_section)
            TRAINING_PHASE -> AppTokens.strings.res(Res.string.personalizations_training_phase_section)
        }
    }
}
