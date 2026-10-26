package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_details_analysis_days
import com.grippo.design.resources.provider.goal_details_analysis_sets
import com.grippo.design.resources.provider.joined_pair
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

    @Composable
    public fun summaryLine(): String {
        val daysLabel = AppTokens.strings.res(Res.plurals.goal_details_analysis_days, days, days)
        val setsLabel = AppTokens.strings.res(Res.plurals.goal_details_analysis_sets, setCount, setCount)
        return AppTokens.strings.res(Res.string.joined_pair, daysLabel, setsLabel)
    }
}
