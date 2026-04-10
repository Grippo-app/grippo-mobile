package com.grippo.data.features.api.local.settings.models

public enum class Range(public val key: String) {
    DAILY("daily"),
    WEEKLY("weekly"),
    LAST_7_DAYS("last_7_days"),
    LAST_14_DAYS("last_14_days"),
    MONTHLY("monthly"),
    LAST_30_DAYS("last_30_days"),
    LAST_60_DAYS("last_60_days"),
    LAST_365_DAYS("last_365_days"),
    YEARLY("yearly");

    public companion object {
        public fun of(key: String?): Range? {
            return entries.firstOrNull { it.key == key }
        }
    }
}
