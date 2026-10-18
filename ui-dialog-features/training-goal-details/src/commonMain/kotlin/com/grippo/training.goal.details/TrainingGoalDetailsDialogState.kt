package com.grippo.training.goal.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.profile.GoalFitFindingState
import com.grippo.core.state.metrics.profile.GoalFitSeverityState
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.data.features.api.metrics.profile.models.GoalAdherence
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_details_analysis_unavailable
import com.grippo.design.resources.provider.goal_details_reason_no_data_detail
import com.grippo.design.resources.provider.goal_details_reason_too_early_detail
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class TrainingGoalDetailsDialogState(
    val progress: GoalProgressState? = null,
) {
    public val priorityFindings: ImmutableList<GoalFitFindingState>
        get() {
            val progress = progress ?: return persistentListOf()
            if (progress.score == null) return persistentListOf()

            return progress.findings
                .filter { it.severity != GoalFitSeverityState.PASS }
                .sortedWith(compareBy({ it.severity.sortKey }, { it.rule.ordinal }))
                .take(2)
                .toPersistentList()
        }

    @Composable
    public fun notice(): String? {
        val progress = progress
        val resource = when {
            progress == null -> Res.string.goal_details_reason_no_data_detail
            progress.score != null -> return null
            progress.evidence.range.value == null -> Res.string.goal_details_analysis_unavailable
            progress.evidence.days < GoalAdherence.MIN_ANALYSIS_DAYS && progress.evidence.setCount > 0 ->
                Res.string.goal_details_reason_too_early_detail

            else -> Res.string.goal_details_reason_no_data_detail
        }
        return AppTokens.strings.res(resource)
    }
}
