package com.grippo.data.features.api.goal.models

public enum class PersonalizationKeyEnum(public val key: String) {
    // 1. Schedule
    LOW_FREQUENCY(key = "low_frequency"),
    MODERATE_FREQUENCY(key = "moderate_frequency"),
    HIGH_FREQUENCY(key = "high_frequency"),
    WEEKDAYS_ONLY(key = "weekdays_only"),
    WEEKENDS_ONLY(key = "weekends_only"),
    FLEXIBLE_SCHEDULE(key = "flexible_schedule"),
    UNPREDICTABLE_SCHEDULE(key = "unpredictable_schedule"),

    // 2. Session duration
    MINUTES_UP_TO_45(key = "minutes_up_to_45"),
    MINUTES_45_TO_90(key = "minutes_45_to_90"),
    MINUTES_90_PLUS(key = "minutes_90_plus"),
    FLEXIBLE_DURATION(key = "flexible_duration"),

    // 3. Lifestyle
    HIGH_STRESS_LIFESTYLE(key = "high_stress_lifestyle"),
    POOR_SLEEP_PATTERN(key = "poor_sleep_pattern"),
    LOW_ENERGY_TENDENCY(key = "low_energy_tendency"),
    SEDENTARY_JOB(key = "sedentary_job"),
    PHYSICALLY_ACTIVE_JOB(key = "physically_active_job"),

    // 4. Recovery context
    RETURNING_AFTER_BREAK(key = "returning_after_break"),
    RECOVERING_FROM_INJURY(key = "recovering_from_injury"),
    LIMITED_RANGE_OF_MOTION(key = "limited_range_of_motion"),
    FATIGUE_MANAGEMENT(key = "fatigue_management"),
    RECOVERY_FOCUS(key = "recovery_focus"),
    MOBILITY_FOCUS(key = "mobility_focus"),

    // 5. Care zones
    NECK_CARE(key = "neck_care"),
    LOWER_BACK_CARE(key = "lower_back_care"),
    UPPER_BACK_CARE(key = "upper_back_care"),
    SHOULDER_CARE(key = "shoulder_care"),
    ELBOW_CARE(key = "elbow_care"),
    WRIST_CARE(key = "wrist_care"),
    HIP_CARE(key = "hip_care"),
    KNEE_CARE(key = "knee_care"),
    ANKLE_CARE(key = "ankle_care"),

    // 6. Health conditions
    HYPERTENSION(key = "hypertension"),
    DIABETES(key = "diabetes"),
    ASTHMA(key = "asthma"),
    OSTEOPOROSIS(key = "osteoporosis"),
    ARTHRITIS(key = "arthritis"),
    HERNIATED_DISC(key = "herniated_disc"),
    CHRONIC_PAIN(key = "chronic_pain"),
    PREGNANCY(key = "pregnancy"),
    POSTPARTUM(key = "postpartum"),

    // 7. Movement restrictions
    AVOID_HIGH_INTENSITY(key = "avoid_high_intensity"),
    AVOID_HEAVY_LOADING(key = "avoid_heavy_loading"),
    AVOID_HIGH_IMPACT(key = "avoid_high_impact"),
    AVOID_DEEP_SQUATS(key = "avoid_deep_squats"),
    AVOID_LUNGES(key = "avoid_lunges"),
    AVOID_OVERHEAD_WORK(key = "avoid_overhead_work"),
    AVOID_HEAVY_SPINAL_LOADING(key = "avoid_heavy_spinal_loading"),
    AVOID_SPINAL_FLEXION(key = "avoid_spinal_flexion"),
    AVOID_TWISTING(key = "avoid_twisting"),
    AVOID_FLOOR_WORK(key = "avoid_floor_work"),
    AVOID_INVERSIONS(key = "avoid_inversions"),
    AVOID_VALSALVA(key = "avoid_valsalva"),
    NEEDS_SUPPORTED_MOVEMENTS(key = "needs_supported_movements"),

    // 8. Sports
    SPORT_POWERLIFTING(key = "sport_powerlifting"),
    SPORT_BODYBUILDING(key = "sport_bodybuilding"),
    SPORT_OLYMPIC_WEIGHTLIFTING(key = "sport_olympic_weightlifting"),
    SPORT_CROSSFIT(key = "sport_crossfit"),
    SPORT_CALISTHENICS(key = "sport_calisthenics"),
    SPORT_RUNNING(key = "sport_running"),
    SPORT_CYCLING(key = "sport_cycling"),
    SPORT_SWIMMING(key = "sport_swimming"),
    SPORT_GRAPPLING(key = "sport_grappling"),
    SPORT_STRIKING(key = "sport_striking"),
    SPORT_TEAM_BALL_SPORTS(key = "sport_team_ball_sports"),
    SPORT_RACQUET_SPORTS(key = "sport_racquet_sports"),
    SPORT_CLIMBING(key = "sport_climbing"),
    SPORT_WINTER_BOARD_SPORTS(key = "sport_winter_board_sports"),
    SPORT_YOGA_OR_PILATES(key = "sport_yoga_or_pilates"),

    // 9. Focus areas
    FOCUS_CHEST(key = "focus_chest"),
    FOCUS_BACK(key = "focus_back"),
    FOCUS_SHOULDERS(key = "focus_shoulders"),
    FOCUS_REAR_DELTS(key = "focus_rear_delts"),
    FOCUS_ARMS(key = "focus_arms"),
    FOCUS_FOREARMS_AND_GRIP(key = "focus_forearms_and_grip"),
    FOCUS_ABS_AND_CORE(key = "focus_abs_and_core"),
    FOCUS_LEGS(key = "focus_legs"),
    FOCUS_GLUTES(key = "focus_glutes"),
    FOCUS_QUADRICEPS(key = "focus_quadriceps"),
    FOCUS_HAMSTRINGS(key = "focus_hamstrings"),
    FOCUS_CALVES(key = "focus_calves"),

    // 10. Physical qualities
    IMPROVE_CARDIO(key = "improve_cardio"),
    IMPROVE_MUSCULAR_ENDURANCE(key = "improve_muscular_endurance"),
    IMPROVE_EXPLOSIVE_POWER(key = "improve_explosive_power"),
    IMPROVE_SPEED(key = "improve_speed"),
    IMPROVE_FLEXIBILITY(key = "improve_flexibility"),
    IMPROVE_BALANCE(key = "improve_balance"),
    IMPROVE_GRIP_STRENGTH(key = "improve_grip_strength"),
    IMPROVE_CORE_STABILITY(key = "improve_core_stability"),

    // 11. Program preferences
    SIMPLE_PLAN(key = "simple_plan"),
    LOWER_VOLUME(key = "lower_volume"),
    HIGHER_VOLUME(key = "higher_volume"),
    GRADUAL_PROGRESSION(key = "gradual_progression"),
    AGGRESSIVE_PROGRESSION(key = "aggressive_progression"),

    // 12. Routine structure
    PREFER_FULL_BODY(key = "prefer_full_body"),
    PREFER_UPPER_LOWER(key = "prefer_upper_lower"),
    PREFER_PUSH_PULL_LEGS(key = "prefer_push_pull_legs"),
    PREFER_SPLIT_ROUTINE(key = "prefer_split_routine"),

    // 13. Set / method preferences
    PREFER_STRAIGHT_SETS(key = "prefer_straight_sets"),
    PREFER_SUPERSETS(key = "prefer_supersets"),
    PREFER_CIRCUITS(key = "prefer_circuits"),
    PREFER_HEAVY_COMPOUNDS(key = "prefer_heavy_compounds"),

    // 14. Equipment preferences
    PREFER_FREE_WEIGHTS(key = "prefer_free_weights"),
    PREFER_MACHINES(key = "prefer_machines"),
    PREFER_CABLES(key = "prefer_cables"),
    PREFER_BODYWEIGHT(key = "prefer_bodyweight"),

    // 15. Training phase
    PHASE_BULKING(key = "phase_bulking"),
    PHASE_CUTTING(key = "phase_cutting"),
    PHASE_MAINTENANCE(key = "phase_maintenance"),
    PHASE_DELOAD(key = "phase_deload"),
    PHASE_COMPETITION_PREP(key = "phase_competition_prep");

    public companion object {
        public fun of(key: String?): PersonalizationKeyEnum? {
            return entries.firstOrNull { it.key == key }
        }
    }
}
