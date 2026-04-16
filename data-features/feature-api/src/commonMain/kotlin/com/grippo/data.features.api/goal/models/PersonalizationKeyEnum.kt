package com.grippo.data.features.api.goal.models

public enum class PersonalizationKeyEnum(public val key: String) {
    // Frequency and schedule
    LOW_FREQUENCY(key = "low_frequency"),
    MODERATE_FREQUENCY(key = "moderate_frequency"),
    HIGH_FREQUENCY(key = "high_frequency"),
    WEEKENDS_ONLY(key = "weekends_only"),
    WEEKDAYS_ONLY(key = "weekdays_only"),
    FLEXIBLE_SCHEDULE(key = "flexible_schedule"),
    INCONSISTENT_SCHEDULE(key = "inconsistent_schedule"),

    // Session duration
    MINUTES_30_45(key = "minutes_30_45"),
    MINUTES_45_60(key = "minutes_45_60"),
    MINUTES_60_90(key = "minutes_60_90"),
    MINUTES_90_120(key = "minutes_90_120"),
    MINUTES_120_PLUS(key = "minutes_120_plus"),
    FLEXIBLE_DURATION(key = "flexible_duration"),

    // Recovery and lifestyle
    LOW_ENERGY_DAYS(key = "low_energy_days"),
    STRESSFUL_PERIOD(key = "stressful_period"),
    POOR_SLEEP(key = "poor_sleep"),
    TRAVELS_OFTEN(key = "travels_often"),

    // Physical limitations
    JOINT_FRIENDLY(key = "joint_friendly"),
    LOW_IMPACT(key = "low_impact"),
    BACK_CARE(key = "back_care"),
    KNEE_CARE(key = "knee_care"),
    SHOULDER_CARE(key = "shoulder_care"),
    WRIST_CARE(key = "wrist_care"),
    RETURNING_AFTER_BREAK(key = "returning_after_break"),
    RECOVERING_FROM_INJURY(key = "recovering_from_injury"),
    RECOVERY_FOCUS(key = "recovery_focus"),
    MOBILITY_FOCUS(key = "mobility_focus"),
    WARMUP_FOCUS(key = "warmup_focus"),
    LIMITED_RANGE_OF_MOTION(key = "limited_range_of_motion"),
    FATIGUE_MANAGEMENT(key = "fatigue_management"),
    AVOID_HIGH_INTENSITY(key = "avoid_high_intensity"),
    AVOID_HEAVY_LOADING(key = "avoid_heavy_loading"),

    // Movement exclusions
    AVOID_DEEP_SQUATS(key = "avoid_deep_squats"),
    AVOID_LUNGES(key = "avoid_lunges"),
    AVOID_OVERHEAD_WORK(key = "avoid_overhead_work"),
    AVOID_HEAVY_SPINAL_LOADING(key = "avoid_heavy_spinal_loading"),
    AVOID_TWISTING(key = "avoid_twisting"),
    AVOID_FLOOR_WORK(key = "avoid_floor_work"),
    NEEDS_SUPPORTED_MOVEMENTS(key = "needs_supported_movements"),

    // Plan style
    SIMPLE_PLAN(key = "simple_plan"),
    MORE_VARIETY(key = "more_variety"),
    LOWER_VOLUME(key = "lower_volume"),
    HIGHER_VOLUME(key = "higher_volume"),
    GRADUAL_PROGRESSION(key = "gradual_progression"),
    AGGRESSIVE_PROGRESSION(key = "aggressive_progression"),
    BEGINNER_FRIENDLY(key = "beginner_friendly"),

    // Structure preferences
    EASY_TO_FOLLOW(key = "easy_to_follow"),
    NO_COMPLEX_MOVEMENTS(key = "no_complex_movements"),
    FULL_BODY_PREFERENCE(key = "full_body_preference"),
    SPLIT_ROUTINE_PREFERENCE(key = "split_routine_preference"),
    UPPER_LOWER_PREFERENCE(key = "upper_lower_preference");

    public companion object {
        public fun of(key: String?): PersonalizationKeyEnum? {
            return entries.firstOrNull { it.key == key }
        }
    }
}
