package com.grippo.domain.state.metrics

import com.grippo.toolkit.date.utils.DateRange
import com.grippo.core.state.metrics.TrainingStreakFeaturedState as StateTrainingStreakFeatured
import com.grippo.core.state.metrics.TrainingStreakKind as StateTrainingStreakKind
import com.grippo.core.state.metrics.TrainingStreakMood as StateTrainingStreakMood
import com.grippo.core.state.metrics.TrainingStreakProgressState as StateTrainingStreakProgress
import com.grippo.core.state.metrics.TrainingStreakState as StateTrainingStreak
import com.grippo.data.features.api.metrics.models.TrainingStreak as DomainTrainingStreak
import com.grippo.data.features.api.metrics.models.TrainingStreakFeatured as DomainTrainingStreakFeatured
import com.grippo.data.features.api.metrics.models.TrainingStreakKind as DomainTrainingStreakKind
import com.grippo.data.features.api.metrics.models.TrainingStreakMood as DomainTrainingStreakMood
import com.grippo.data.features.api.metrics.models.TrainingStreakProgressEntry as DomainTrainingStreakProgress

public fun DomainTrainingStreak.toState(): StateTrainingStreak {
    return StateTrainingStreak(
        totalActiveDays = totalActiveDays,
        featured = featured.toState(),
        timeline = timeline.map(DomainTrainingStreakProgress::toState),
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
            mask = mask,
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
        range = DateRange(from = from, to = to),
    )
}
