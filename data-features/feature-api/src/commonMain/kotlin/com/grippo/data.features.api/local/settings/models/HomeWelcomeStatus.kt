package com.grippo.data.features.api.local.settings.models

public enum class HomeWelcomeStatus(public val key: String) {
    Idle("idle"),
    PendingCelebration("pending_celebration"),
    Celebrated("celebrated");

    public companion object {
        public fun of(key: String?): HomeWelcomeStatus =
            entries.firstOrNull { it.key == key } ?: Idle
    }
}
