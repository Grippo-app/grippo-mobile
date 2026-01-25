package com.grippo.data.features.api.user.models

public enum class RoleEnum(public val key: String) {
    DEFAULT(key = "default"),
    ADMIN(key = "admin");

    public companion object {
        public fun of(key: String?): RoleEnum {
            return entries.firstOrNull { it.key == key } ?: DEFAULT
        }
    }
}