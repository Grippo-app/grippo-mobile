package com.grippo.data.features.api.muscle.models

public enum class MuscleStatusEnum(private val key: String) {
    INCLUDED("included"),
    EXCLUDED("excluded"),
    UNIDENTIFIED("unidentified");

    public companion object {
        public fun of(key: String?): MuscleStatusEnum {
            return entries.firstOrNull { it.key == key } ?: UNIDENTIFIED
        }
    }
}