package com.grippo.domain.state.metrics.performance

import com.grippo.domain.state.exercise.example.toState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlin.math.min
import com.grippo.core.state.metrics.performance.ExerciseSpotlightState as StateExerciseSpotlight
import com.grippo.data.features.api.metrics.performance.models.ExerciseSpotlight as DomainExerciseSpotlight

public fun List<DomainExerciseSpotlight>.toState(): ImmutableList<StateExerciseSpotlight> {
    return map { it.toState() }.toPersistentList()
}

public fun DomainExerciseSpotlight.toState(): StateExerciseSpotlight {
    return when (this) {
        is DomainExerciseSpotlight.ProgressWin -> StateExerciseSpotlight(
            kind = StateExerciseSpotlight.Kind.ProgressWin,
            severity = StateExerciseSpotlight.Severity.Positive,
            example = example.toState(),
            details = StateExerciseSpotlight.Details.ProgressWin(
                improvementPercent = improvementPercent,
                comparedSessions = min(5, sampleSize - 1),
                latestSessionVolume = latestSessionVolume,
                baselineVolumeMedian = baselineVolumeMedian,
            ),
            confidence = confidence,
            score = score,
        )

        is DomainExerciseSpotlight.NeedsAttention -> StateExerciseSpotlight(
            kind = StateExerciseSpotlight.Kind.NeedsAttention,
            severity = StateExerciseSpotlight.Severity.Warning,
            example = example.toState(),
            details = StateExerciseSpotlight.Details.NeedsAttention(
                currentGapDays = currentGapDays,
                typicalGapDays = typicalGapDays,
                triggerGapDays = triggerGapDays,
            ),
            confidence = confidence,
            score = score,
        )

        is DomainExerciseSpotlight.GoodFrequency -> StateExerciseSpotlight(
            kind = StateExerciseSpotlight.Kind.GoodFrequency,
            severity = StateExerciseSpotlight.Severity.Positive,
            example = example.toState(),
            details = StateExerciseSpotlight.Details.GoodFrequency(
                avgWeeklyFrequency = avgWeeklyFrequency,
                activeWeeks = activeWeeks,
                appearancesInWindow = appearancesInWindow,
                recentWindowDays = recentWindowDays,
            ),
            confidence = confidence,
            score = score,
        )

        is DomainExerciseSpotlight.NearBest -> StateExerciseSpotlight(
            kind = StateExerciseSpotlight.Kind.NearBest,
            severity = StateExerciseSpotlight.Severity.Positive,
            example = example.toState(),
            details = StateExerciseSpotlight.Details.NearBest(
                gapPercent = gapPercent,
                latestSessionVolume = latestSessionVolume,
                bestSessionVolume = bestSessionVolume,
            ),
            confidence = confidence,
            score = score,
        )
    }
}
