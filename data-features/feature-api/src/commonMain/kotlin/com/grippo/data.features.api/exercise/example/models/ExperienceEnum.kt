package com.grippo.data.features.api.exercise.example.models

public enum class ExperienceEnum(public val key: String) {
    BEGINNER(key = "beginner"),
    INTERMEDIATE(key = "intermediate"),
    ADVANCED(key = "advanced"),
    PRO(key = "pro");

    public companion object {
        public fun of(key: String?): ExperienceEnum? {
            return entries.firstOrNull { it.key == key }
        }
    }
}