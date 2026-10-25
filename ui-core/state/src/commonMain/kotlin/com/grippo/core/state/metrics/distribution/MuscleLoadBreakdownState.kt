package com.grippo.core.state.metrics.distribution

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.design.core.AppTokens
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

@Immutable
public data class MuscleLoadBreakdownState(
    val entries: ImmutableList<MuscleLoadEntryState>,
) {
    @Composable
    public fun color(entry: MuscleLoadEntryState): Color {
        val muscleColors = AppTokens.colors.muscle
        val palette = muscleColors.palette6MuscleCalm
        if (entries.isEmpty() || palette.isEmpty()) return muscleColors.inactive
        if (entries.size == 1) return palette.last()

        val minimum = entries.minOf { it.value }
        val maximum = entries.maxOf { it.value }
        val range = (maximum - minimum).takeIf { it > 0f } ?: 1f
        val relativeShare = ((entry.value - minimum) / range).coerceIn(0f, 1f)
        val index = (relativeShare * (palette.size - 1)).roundToInt()
            .coerceIn(0, palette.size - 1)
        return palette[index]
    }
}
