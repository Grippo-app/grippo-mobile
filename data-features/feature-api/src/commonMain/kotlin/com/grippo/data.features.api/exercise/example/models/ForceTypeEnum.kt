package com.grippo.data.features.api.exercise.example.models

public enum class ForceTypeEnum(private val key: String) {
    PULL(key = "pull"),
    PUSH(key = "push"),
    HINGE(key = "hinge");

    public companion object {
        public fun of(key: String?): ForceTypeEnum? {
            return entries.firstOrNull { it.key == key }
        }
    }
}