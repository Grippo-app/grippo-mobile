package com.grippo.domain.state.metrics

import com.grippo.core.state.metrics.TrainingStreakFeaturedState as StateTrainingStreakFeatured
import com.grippo.core.state.metrics.TrainingStreakMood as StateTrainingStreakMood
import com.grippo.core.state.metrics.TrainingStreakProgressState as StateTrainingStreakProgress
import com.grippo.core.state.metrics.TrainingStreakRhythmState as StateTrainingStreakRhythm
import com.grippo.core.state.metrics.TrainingStreakState as StateTrainingStreak
import com.grippo.core.state.metrics.TrainingStreakType as StateTrainingStreakType
import com.grippo.data.features.api.metrics.models.TrainingStreak as DomainTrainingStreak
import com.grippo.data.features.api.metrics.models.TrainingStreakFeatured as DomainTrainingStreakFeatured
import com.grippo.data.features.api.metrics.models.TrainingStreakMood as DomainTrainingStreakMood
import com.grippo.data.features.api.metrics.models.TrainingStreakProgressEntry as DomainTrainingStreakProgress
import com.grippo.data.features.api.metrics.models.TrainingStreakRhythm as DomainTrainingStreakRhythm
import com.grippo.data.features.api.metrics.models.TrainingStreakType as DomainTrainingStreakType

public fun DomainTrainingStreak.toState(): StateTrainingStreak {
    return StateTrainingStreak(
        totalActiveDays = totalActiveDays,
        featured = featured.toState(),
        timeline = timeline.map(DomainTrainingStreakProgress::toState),
    )
}

private fun DomainTrainingStreakFeatured.toState(): StateTrainingStreakFeatured {
    return StateTrainingStreakFeatured(
        type = type.toState(),
        length = length,
        targetSessionsPerPeriod = targetSessionsPerPeriod,
        periodLengthDays = periodLengthDays,
        mood = mood.toState(),
        progressPercent = progressPercent,
        rhythm = rhythm?.toState(),
    )
}

private fun DomainTrainingStreakType.toState(): StateTrainingStreakType {
    return when (this) {
        DomainTrainingStreakType.Daily -> StateTrainingStreakType.Daily
        DomainTrainingStreakType.Weekly -> StateTrainingStreakType.Weekly
        DomainTrainingStreakType.Rhythm -> StateTrainingStreakType.Rhythm
    }
}

private fun DomainTrainingStreakMood.toState(): StateTrainingStreakMood {
    return when (this) {
        DomainTrainingStreakMood.CrushingIt -> StateTrainingStreakMood.CrushingIt
        DomainTrainingStreakMood.OnTrack -> StateTrainingStreakMood.OnTrack
        DomainTrainingStreakMood.Restart -> StateTrainingStreakMood.Restart
    }
}

private fun DomainTrainingStreakRhythm.toState(): StateTrainingStreakRhythm {
    return StateTrainingStreakRhythm(
        workDays = workDays,
        restDays = restDays,
    )
}

private fun DomainTrainingStreakProgress.toState(): StateTrainingStreakProgress {
    return StateTrainingStreakProgress(
        progressPercent = progressPercent,
        achievedSessions = achievedSessions,
        targetSessions = targetSessions,
    )
}
