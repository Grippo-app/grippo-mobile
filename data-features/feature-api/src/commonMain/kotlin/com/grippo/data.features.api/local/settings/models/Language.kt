package com.grippo.data.features.api.local.settings.models

public enum class Language(public val key: String) {
    ENGLISH("en"),
    UKRAINIAN("uk"),
    RUSSIAN("ru"),
    PORTUGUESE_BRAZIL("pt-BR"),
    PORTUGUESE_PORTUGAL("pt-PT");

    public companion object {
        public fun of(key: String?): Language? {
            return entries.firstOrNull { it.key == key }
        }
    }
}
