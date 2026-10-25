package com.grippo.domain.state.metrics.engagement

import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.toPersistentList
import com.grippo.core.state.metrics.engagement.TrainingStreakFeaturedState as StateTrainingStreakFeatured
import com.grippo.core.state.metrics.engagement.TrainingStreakMood as StateTrainingStreakMood
import com.grippo.core.state.metrics.engagement.TrainingStreakProgressState as StateTrainingStreakProgress
import com.grippo.core.state.metrics.engagement.TrainingStreakState as StateTrainingStreak
import com.grippo.data.features.api.metrics.engagement.models.TrainingStreak as DomainTrainingStreak
import com.grippo.data.features.api.metrics.engagement.models.TrainingStreakFeatured as DomainTrainingStreakFeatured
import com.grippo.data.features.api.metrics.engagement.models.TrainingStreakMood as DomainTrainingStreakMood
import com.grippo.data.features.api.metrics.engagement.models.TrainingStreakProgressEntry as DomainTrainingStreakProgress

public fun DomainTrainingStreak.toState(): StateTrainingStreak {
    return StateTrainingStreak(
        totalActiveDays = totalActiveDays,
        featured = featured.toState(),
        timeline = timeline
            .map(DomainTrainingStreakProgress::toState)
            .toPersistentList(),
        historyDays = historyDays,
        lastTrainingGapDays = lastTrainingGapDays,
        nextTrainingDate = nextTrainingDate?.let { date ->
            DateFormatState.of(
                value = date,
                range = DateRangePresets.infinity(),
                format = DateFormat.DateOnly.DateDdMmm,
            )
        },
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
            targetTrainingDaysPerPeriod = targetTrainingDaysPerWeek,
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

private fun DomainTrainingStreakProgress.toState(): StateTrainingStreakProgress {
    return StateTrainingStreakProgress(
        achievedTrainingDays = achievedTrainingDays,
        targetTrainingDays = targetTrainingDays,
        isPartial = isPartial,
        range = DateRangeFormatState.of(DateRange(from = from, to = to)),
    )
}
