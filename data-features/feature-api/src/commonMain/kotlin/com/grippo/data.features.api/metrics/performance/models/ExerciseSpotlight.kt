package com.grippo.data.features.api.metrics.performance.models

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import kotlinx.datetime.LocalDateTime

public sealed interface ExerciseSpotlight {
    public val kind: Kind
    public val severity: Severity
    public val subjectId: String
    public val subjectLabel: String
    public val example: ExerciseExampleValue
    public val confidence: Float
    public val score: Float
    public val sampleSize: Int
    public val lastSeenAt: LocalDateTime

    public enum class Kind {
        ProgressWin,
        NeedsAttention,
        GoodFrequency,
        NearBest,
    }

    public enum class Severity {
        Positive,
        Warning,
    }

    public data class ProgressWin(
        override val subjectId: String,
        override val subjectLabel: String,
        override val example: ExerciseExampleValue,
        override val confidence: Float,
        override val score: Float,
        override val sampleSize: Int,
        override val lastSeenAt: LocalDateTime,
        val baselineVolumeMedian: Float,
        val latestSessionVolume: Float,
        val improvementPercent: Int,
    ) : ExerciseSpotlight {
        override val kind: Kind = Kind.ProgressWin
        override val severity: Severity = Severity.Positive
    }

    public data class NeedsAttention(
        override val subjectId: String,
        override val subjectLabel: String,
        override val example: ExerciseExampleValue,
        override val confidence: Float,
        override val score: Float,
        override val sampleSize: Int,
        override val lastSeenAt: LocalDateTime,
        val typicalGapDays: Int,
        val currentGapDays: Int,
        val triggerGapDays: Int,
    ) : ExerciseSpotlight {
        override val kind: Kind = Kind.NeedsAttention
        override val severity: Severity = Severity.Warning
    }

    public data class GoodFrequency(
        override val subjectId: String,
        override val subjectLabel: String,
        override val example: ExerciseExampleValue,
        override val confidence: Float,
        override val score: Float,
        override val sampleSize: Int,
        override val lastSeenAt: LocalDateTime,
        val appearancesInWindow: Int,
        val activeWeeks: Int,
        val avgWeeklyFrequency: Float,
        val recentWindowDays: Int,
    ) : ExerciseSpotlight {
        override val kind: Kind = Kind.GoodFrequency
        override val severity: Severity = Severity.Positive
    }

    public data class NearBest(
        override val subjectId: String,
        override val subjectLabel: String,
        override val example: ExerciseExampleValue,
        override val confidence: Float,
        override val score: Float,
        override val sampleSize: Int,
        override val lastSeenAt: LocalDateTime,
        val bestSessionVolume: Float,
        val latestSessionVolume: Float,
        val gapPercent: Int,
    ) : ExerciseSpotlight {
        override val kind: Kind = Kind.NearBest
        override val severity: Severity = Severity.Positive
    }
}
