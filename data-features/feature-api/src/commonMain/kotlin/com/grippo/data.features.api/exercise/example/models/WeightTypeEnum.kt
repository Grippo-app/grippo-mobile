package com.grippo.data.features.api.exercise.example.models

public enum class WeightTypeEnum(private val key: String) {
    FREE(key = "free"),
    FIXED(key = "fixed"),
    BODY_WEIGHT(key = "body_weight");

    public companion object {
        public fun of(key: String?): WeightTypeEnum? {
            return entries.firstOrNull { it.key == key }
        }
    }
}