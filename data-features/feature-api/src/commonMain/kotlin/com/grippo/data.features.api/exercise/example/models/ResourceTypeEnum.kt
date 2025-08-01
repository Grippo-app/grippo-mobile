package com.grippo.data.features.api.exercise.example.models

public enum class ResourceTypeEnum(public val key: String) {
    YOUTUBE_VIDEO("youtube_video"),
    VIDEO("video"),
    TEXT("text");

    public companion object {
        public fun of(key: String?): ResourceTypeEnum? {
            return entries.firstOrNull { it.key == key }
        }
    }
}