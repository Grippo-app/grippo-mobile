package com.grippo.domain.state.metrics.distribution

import com.grippo.domain.state.muscles.toState
import kotlinx.collections.immutable.toPersistentList
import com.grippo.core.state.metrics.distribution.MuscleLoadBreakdownState as StateMuscleLoadBreakdown
import com.grippo.core.state.metrics.distribution.MuscleLoadEntryState as StateMuscleLoadEntry
import com.grippo.data.features.api.metrics.distribution.models.MuscleLoadBreakdown as DomainMuscleLoadBreakdown
import com.grippo.data.features.api.metrics.distribution.models.MuscleLoadEntry as DomainMuscleLoadEntry

public fun DomainMuscleLoadBreakdown.toState(): StateMuscleLoadBreakdown {
    val entries = entries
        .map(DomainMuscleLoadEntry::toState).toPersistentList()
    return StateMuscleLoadBreakdown(entries = entries)
}

private fun DomainMuscleLoadEntry.toState(): StateMuscleLoadEntry {
    val normalizedValue = percentage.coerceIn(0f, 100f)
    return StateMuscleLoadEntry(
        group = group.toState(),
        value = normalizedValue,
        muscles = muscles.map { it.toState() }.toPersistentList(),
        hitTrainingsCount = hitTrainingsCount,
        primaryTrainingsCount = primaryTrainingsCount,
        avgStimulusPerHitSession = avgStimulusPerHitSession,
        maxStimulusInOneSession = maxStimulusInOneSession,
        avgVolumePerHitSession = avgVolumePerHitSession,
        maxVolumeInOneSession = maxVolumeInOneSession,
        topExampleIds = topExampleIds.toPersistentList(),
    )
}
