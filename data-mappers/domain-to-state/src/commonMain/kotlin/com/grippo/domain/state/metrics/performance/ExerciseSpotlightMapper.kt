package com.grippo.domain.state.metrics.performance

import com.grippo.domain.state.exercise.example.toState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import com.grippo.core.state.metrics.performance.ExerciseSpotlightState as StateExerciseSpotlight
import com.grippo.data.features.api.metrics.performance.models.ExerciseSpotlight as DomainExerciseSpotlight

public fun List<DomainExerciseSpotlight>.toState(): ImmutableList<StateExerciseSpotlight> {
    return map { it.toState() }.toPersistentList()
}

public fun DomainExerciseSpotlight.toState(): StateExerciseSpotlight {
    return when (this) {
        is DomainExerciseSpotlight.ProgressWin -> StateExerciseSpotlight(
            example = example.toState(),
            details = StateExerciseSpotlight.Details.ProgressWin(
                improvementPercent = improvementPercent,
                comparedSessions = comparedSessions,
                comparisonWindowDays = comparisonWindowDays,
                latestSessionVolume = latestSessionVolume,
                baselineVolumeMedian = baselineVolumeMedian,
            ),
            confidence = confidence,
            score = score,
        )

        is DomainExerciseSpotlight.NeedsAttention -> StateExerciseSpotlight(
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
            example = example.toState(),
            details = StateExerciseSpotlight.Details.NearBest(
                gapPercent = gapPercent,
                comparisonWindowDays = comparisonWindowDays,
                latestSessionVolume = latestSessionVolume,
                bestSessionVolume = bestSessionVolume,
            ),
            confidence = confidence,
            score = score,
        )
    }
}
