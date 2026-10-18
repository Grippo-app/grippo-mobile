package com.grippo.core.state.metrics.engagement

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDateTime
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

public fun stubTrainingStreakProgress(): TrainingStreakProgressState {
    return TrainingStreakProgressState(
        achievedTrainingDays = 4,
        targetTrainingDays = 4,
        isPartial = false,
        range = DateRangeFormatState.of(
            from = LocalDateTime(2026, 10, 12, 0, 0),
            to = LocalDateTime(2026, 10, 18, 23, 59),
        ),
    )
}
