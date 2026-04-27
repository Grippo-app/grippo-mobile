package com.grippo.domain.state.metrics.engagement

import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.toPersistentList
import com.grippo.core.state.metrics.engagement.TrainingStreakFeaturedState as StateTrainingStreakFeatured
import com.grippo.core.state.metrics.engagement.TrainingStreakKind as StateTrainingStreakKind
import com.grippo.core.state.metrics.engagement.TrainingStreakMood as StateTrainingStreakMood
import com.grippo.core.state.metrics.engagement.TrainingStreakProgressState as StateTrainingStreakProgress
import com.grippo.core.state.metrics.engagement.TrainingStreakState as StateTrainingStreak
import com.grippo.data.features.api.metrics.engagement.models.TrainingStreak as DomainTrainingStreak
import com.grippo.data.features.api.metrics.engagement.models.TrainingStreakFeatured as DomainTrainingStreakFeatured
import com.grippo.data.features.api.metrics.engagement.models.TrainingStreakKind as DomainTrainingStreakKind
import com.grippo.data.features.api.metrics.engagement.models.TrainingStreakMood as DomainTrainingStreakMood
import com.grippo.data.features.api.metrics.engagement.models.TrainingStreakProgressEntry as DomainTrainingStreakProgress

public fun DomainTrainingStreak.toState(): StateTrainingStreak {
    return StateTrainingStreak(
        totalActiveDays = totalActiveDays,
        featured = featured.toState(),
        timeline = timeline
            .map(DomainTrainingStreakProgress::toState)
            .toPersistentList(),
        kind = kind.toState(),
        score = score,
        historyDays = historyDays,
        lastTrainingGapDays = lastTrainingGapDays,
    )
}

private fun DomainTrainingStreakFeatured.toState(): StateTrainingStreakFeatured {
    return when (this) {
        is DomainTrainingStreakFeatured.Daily -> StateTrainingStreakFeatured.Daily(
            length = length,
            mood = mood.toState(),
            progressPercent = progressPercent,
            confidence = confidence,
        )

        is DomainTrainingStreakFeatured.Weekly -> StateTrainingStreakFeatured.Weekly(
            length = length,
            targetSessionsPerPeriod = targetSessionsPerWeek,
            mood = mood.toState(),
            progressPercent = progressPercent,
            confidence = confidence,
        )

        is DomainTrainingStreakFeatured.Rhythm -> StateTrainingStreakFeatured.Rhythm(
            length = length,
            workDays = workDays,
            restDays = restDays,
            mood = mood.toState(),
            progressPercent = progressPercent,
            confidence = confidence,
        )

        is DomainTrainingStreakFeatured.Pattern -> StateTrainingStreakFeatured.Pattern(
            length = length,
            targetSessionsPerPeriod = targetSessionsPerPeriod,
            periodLengthDays = periodLengthDays,
            mask = mask.toPersistentList(),
            mood = mood.toState(),
            progressPercent = progressPercent,
            confidence = confidence,
        )
    }
}

private fun DomainTrainingStreakMood.toState(): StateTrainingStreakMood {
    return when (this) {
        DomainTrainingStreakMood.CrushingIt -> StateTrainingStreakMood.CrushingIt
        DomainTrainingStreakMood.OnTrack -> StateTrainingStreakMood.OnTrack
        DomainTrainingStreakMood.Paused -> StateTrainingStreakMood.Paused
        DomainTrainingStreakMood.Restart -> StateTrainingStreakMood.Restart
    }
}

private fun DomainTrainingStreakKind.toState(): StateTrainingStreakKind {
    return when (this) {
        DomainTrainingStreakKind.Daily -> StateTrainingStreakKind.Daily
        DomainTrainingStreakKind.Weekly -> StateTrainingStreakKind.Weekly
        DomainTrainingStreakKind.Rhythm -> StateTrainingStreakKind.Rhythm
        DomainTrainingStreakKind.Pattern -> StateTrainingStreakKind.Pattern
    }
}

private fun DomainTrainingStreakProgress.toState(): StateTrainingStreakProgress {
    return StateTrainingStreakProgress(
        progressPercent = progressPercent,
        achievedSessions = achievedSessions,
        targetSessions = targetSessions,
        range = DateRangeFormatState.of(DateRange(from = from, to = to)),
    )
}
