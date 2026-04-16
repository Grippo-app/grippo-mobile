package com.grippo.data.features.api.goal.models

public enum class GoalPrimaryGoalEnum(public val key: String) {
    BUILD_MUSCLE(key = "build_muscle"),
    GET_STRONGER(key = "get_stronger"),
    LOSE_FAT(key = "lose_fat"),
    RETURN_TO_TRAINING(key = "return_to_training"),
    MAINTAIN(key = "maintain"),
    GENERAL_FITNESS(key = "general_fitness");

    public companion object {
        public fun of(key: String?): GoalPrimaryGoalEnum? {
            return entries.firstOrNull { it.key == key }
        }
    }
}
