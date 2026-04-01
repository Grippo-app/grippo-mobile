package com.grippo.screen.api.deeplink

public enum class Deeplink(public val key: String) {
    TrainingDraft(key = "training_draft"),
    WeightHistory(key = "weight_history");

    public companion object {
        public fun fromKey(key: String): Deeplink? = entries.find { it.key == key }
    }
}
