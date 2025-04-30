package com.grippo.data.features.api.muscle.models

public enum class MuscleLoadEnum(private val key: String) {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low"),
    UNIDENTIFIED("unidentified");

    override fun toString(): String {
        return key
    }

    public companion object {
        public fun of(key: String?): MuscleLoadEnum {
            return entries.firstOrNull { it.key == key } ?: UNIDENTIFIED
        }
    }
}