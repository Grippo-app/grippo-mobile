package com.grippo.data.features.api.metrics.engagement.models

public sealed interface TrainingStreakFeatured {
    public val length: Int
    public val mood: TrainingStreakMood
    public val progressPercent: Int
    public val confidence: Float

    public data class Daily(
        override val length: Int,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeatured

    public data class Weekly(
        override val length: Int,
        val targetTrainingDaysPerWeek: Int,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeatured

    public data class Rhythm(
        override val length: Int,
        val workDays: Int,
        val restDays: Int,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeatured

    public data class Pattern(
        override val length: Int,
        val mask: List<Boolean>,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeatured {
        public val targetTrainingDaysPerPeriod: Int
            get() = mask.count { it }

        public val periodLengthDays: Int
            get() = mask.size
    }
}
