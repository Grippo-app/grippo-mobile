package com.grippo.data.features.api.exercise.example.models

public enum class CategoryEnum(private val key: String) {
    COMPOUND(key = "compound"),
    ISOLATION(key = "isolation");

    public companion object {
        public fun of(key: String?): CategoryEnum? {
            return entries.firstOrNull { it.key == key }
        }
    }
}