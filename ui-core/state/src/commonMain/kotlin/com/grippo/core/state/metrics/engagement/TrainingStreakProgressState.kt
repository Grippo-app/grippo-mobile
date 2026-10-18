package com.grippo.core.state.metrics.engagement

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt
import com.grippo.core.state.formatters.DateRangeFormatState

@Immutable
public data class TrainingStreakProgressState(
    val achievedTrainingDays: Int,
    val targetTrainingDays: Int,
    val isPartial: Boolean,
    val range: DateRangeFormatState,
) {
    public val progressPercent: Int
        get() = if (targetTrainingDays > 0) {
            (achievedTrainingDays.toFloat() / targetTrainingDays).coerceIn(0f, 1f).times(100f).roundToInt()
        } else {
            0
        }
}
