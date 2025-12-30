package com.grippo.domain.state.muscles.metrics

import com.grippo.domain.state.muscles.toState
import kotlinx.collections.immutable.toPersistentList
import com.grippo.core.state.muscles.metrics.MuscleLoadBreakdown as StateMuscleLoadBreakdown
import com.grippo.core.state.muscles.metrics.MuscleLoadEntry as StateMuscleLoadEntry
import com.grippo.data.features.api.muscle.models.MuscleLoadBreakdown as DomainMuscleLoadBreakdown
import com.grippo.data.features.api.muscle.models.MuscleLoadEntry as DomainMuscleLoadEntry

public fun DomainMuscleLoadBreakdown.toState(): StateMuscleLoadBreakdown {
    val entries = entries.map(DomainMuscleLoadEntry::toState)
    return StateMuscleLoadBreakdown(entries = entries)
}

private fun DomainMuscleLoadEntry.toState(): StateMuscleLoadEntry {
    val normalizedValue = percentage.coerceIn(0f, 100f)
    return StateMuscleLoadEntry(
        label = label,
        value = normalizedValue,
        muscles = muscles.map { it.toState() }.toPersistentList(),
    )
}