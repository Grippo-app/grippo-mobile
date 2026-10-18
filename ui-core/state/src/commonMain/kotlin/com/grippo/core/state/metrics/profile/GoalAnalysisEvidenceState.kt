package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import kotlinx.datetime.daysUntil

@Immutable
public data class GoalAnalysisEvidenceState(
    val range: DateRangeFormatState,
    val setCount: Int,
    val excludedSetCount: Int,
    val missingMetadataSetCount: Int,
) {
    public val days: Int
        get() {
            val value = range.value ?: return 0
            return value.from.date.daysUntil(value.to.date) + 1
        }
}
