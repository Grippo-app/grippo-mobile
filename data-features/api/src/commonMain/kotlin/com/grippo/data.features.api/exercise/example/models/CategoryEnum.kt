package com.grippo.data.features.api.exercise.example.models

public enum class CategoryEnum(private val key: String) {
    COMPOUND(key = "compound"),
    ISOLATION(key = "isolation"),
    UNIDENTIFIED(key = "unidentified");


    override fun toString(): String {
        return key
    }

    public companion object {
        public fun of(key: String?): CategoryEnum {
            return entries.firstOrNull { it.key == key } ?: UNIDENTIFIED
        }
    }
}