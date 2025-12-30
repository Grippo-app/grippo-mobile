package com.grippo.domain.state.muscles.metrics

import androidx.compose.ui.graphics.Color
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.domain.state.muscles.toState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import com.grippo.core.state.muscles.metrics.MuscleLoadBreakdown as StateMuscleLoadBreakdown
import com.grippo.core.state.muscles.metrics.MuscleLoadEntry as StateMuscleLoadEntry
import com.grippo.core.state.muscles.metrics.MuscleLoadSummary as StateMuscleLoadSummary
import com.grippo.data.features.api.muscle.models.MuscleLoadBreakdown as DomainMuscleLoadBreakdown
import com.grippo.data.features.api.muscle.models.MuscleLoadEntry as DomainMuscleLoadEntry
import com.grippo.data.features.api.muscle.models.MuscleLoadSummary as DomainMuscleLoadSummary

private val fallbackPalette: List<Color> = listOf(
    Color(0xFFF57F17), // Amber
    Color(0xFFFF7F50), // Coral
    Color(0xFFFF7A29), // Orange
    Color(0xFFFF4C4C), // Red
    Color(0xFF880E4F), // Burgundy
)

public fun DomainMuscleLoadSummary.toState(): StateMuscleLoadSummary {
    return StateMuscleLoadSummary(
        perGroup = perGroup.toState(),
        images = null,
    )
}

public fun DomainMuscleLoadSummary.perMuscleState(): StateMuscleLoadBreakdown {
    return perMuscle.toState()
}

public fun DomainMuscleLoadBreakdown.toState(): StateMuscleLoadBreakdown {
    return StateMuscleLoadBreakdown(entries = entries.map(::mapEntry))
}

private fun mapEntry(entry: DomainMuscleLoadEntry): StateMuscleLoadEntry {
    val normalizedValue = entry.percentage.coerceIn(0f, 100f)
    return StateMuscleLoadEntry(
        label = entry.label,
        value = normalizedValue,
        color = colorByPercentage(normalizedValue),
        muscles = entry.muscles.toStateList(),
    )
}

private fun colorByPercentage(percentage: Float): Color {
    if (fallbackPalette.isEmpty()) return Color(0xFFFF4C4C)
    val normalized = percentage.coerceIn(0f, 100f)
    val bandWidth = 100f / fallbackPalette.size
    val index = (normalized / bandWidth).toInt().coerceIn(0, fallbackPalette.size - 1)
    return fallbackPalette[index]
}

private fun List<MuscleEnum>.toStateList(): ImmutableList<MuscleEnumState> {
    return map(MuscleEnum::toState).toPersistentList()
}
