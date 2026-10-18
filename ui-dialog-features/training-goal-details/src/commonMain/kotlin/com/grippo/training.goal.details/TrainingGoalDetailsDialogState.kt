package com.grippo.training.goal.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.data.features.api.metrics.profile.models.GoalAdherence
import com.grippo.design.components.metrics.profile.goal.GoalInsightSeverity
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_details_reason_almost_done_detail
import com.grippo.design.resources.provider.goal_details_reason_almost_done_title
import com.grippo.design.resources.provider.goal_details_reason_drifting_detail
import com.grippo.design.resources.provider.goal_details_reason_drifting_title
import com.grippo.design.resources.provider.goal_details_reason_no_data_detail
import com.grippo.design.resources.provider.goal_details_reason_no_data_title
import com.grippo.design.resources.provider.goal_details_reason_off_track_detail
import com.grippo.design.resources.provider.goal_details_reason_off_track_title
import com.grippo.design.resources.provider.goal_details_reason_on_track_detail
import com.grippo.design.resources.provider.goal_details_reason_on_track_title
import com.grippo.design.resources.provider.goal_details_reason_overdue_detail
import com.grippo.design.resources.provider.goal_details_reason_overdue_title
import com.grippo.design.resources.provider.goal_details_reason_too_early_detail
import com.grippo.design.resources.provider.goal_details_reason_too_early_title
import com.grippo.design.resources.provider.goal_details_tip_review_deadline
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class TrainingGoalDetailsDialogState(
    val progress: GoalProgressState? = null,
) {
    public val insights: ImmutableList<InsightItem>
        get() {
            val progress = progress ?: return persistentListOf(
                InsightItem(severity = InsightItem.Severity.Neutral, reason = ReasonCode.NoData)
            )
            val score = progress.score
            val evidence = progress.evidence
            val summary = when {
                score == null && evidence.days < GoalAdherence.MIN_ANALYSIS_DAYS && evidence.setCount > 0 ->
                    InsightItem(severity = InsightItem.Severity.Neutral, reason = ReasonCode.TooEarly)

                score == null ->
                    InsightItem(severity = InsightItem.Severity.Neutral, reason = ReasonCode.NoData)

                score >= GoalProgressState.ON_TRACK_MIN ->
                    InsightItem(severity = InsightItem.Severity.Positive, reason = ReasonCode.ScoreOnTrack)

                score >= GoalProgressState.DRIFTING_MIN ->
                    InsightItem(severity = InsightItem.Severity.Warning, reason = ReasonCode.ScoreDrifting)

                else ->
                    InsightItem(severity = InsightItem.Severity.Negative, reason = ReasonCode.ScoreOffTrack)
            }
            val timeline = when {
                progress.daysRemaining < 0 -> InsightItem(
                    severity = InsightItem.Severity.Warning,
                    reason = ReasonCode.Overdue,
                    action = TipCode.ReviewDeadline,
                )

                progress.progressFraction >= GoalAdherence.ALMOST_DONE_FRACTION -> InsightItem(
                    severity = InsightItem.Severity.Neutral,
                    reason = ReasonCode.AlmostDone,
                )

                else -> null
            }

            return listOfNotNull(summary, timeline).toPersistentList()
        }

    @Immutable
    public data class InsightItem(
        val severity: Severity,
        val reason: ReasonCode,
        val action: TipCode? = null,
    ) {
        @Immutable
        public enum class Severity {
            /** Something is working well - green. */
            Positive,

            /** Something drifting - amber. */
            Warning,

            /** Something is clearly off - red. */
            Negative,

            /** Neutral, informational - muted. */
            Neutral;

            public fun goalInsightSeverity(): GoalInsightSeverity = when (this) {
                Positive -> GoalInsightSeverity.Positive
                Warning -> GoalInsightSeverity.Warning
                Negative -> GoalInsightSeverity.Negative
                Neutral -> GoalInsightSeverity.Neutral
            }
        }
    }

    @Immutable
    public enum class ReasonCode {
        NoData,
        TooEarly,
        Overdue,
        AlmostDone,
        ScoreOnTrack,
        ScoreDrifting,
        ScoreOffTrack;

        @Composable
        public fun reasonText(): Pair<String, String> = when (this) {
            NoData -> AppTokens.strings.res(Res.string.goal_details_reason_no_data_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_no_data_detail)

            TooEarly -> AppTokens.strings.res(Res.string.goal_details_reason_too_early_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_too_early_detail)

            Overdue -> AppTokens.strings.res(Res.string.goal_details_reason_overdue_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_overdue_detail)

            AlmostDone -> AppTokens.strings.res(Res.string.goal_details_reason_almost_done_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_almost_done_detail)

            ScoreOnTrack -> AppTokens.strings.res(Res.string.goal_details_reason_on_track_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_on_track_detail)

            ScoreDrifting -> AppTokens.strings.res(Res.string.goal_details_reason_drifting_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_drifting_detail)

            ScoreOffTrack -> AppTokens.strings.res(Res.string.goal_details_reason_off_track_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_off_track_detail)
        }
    }

    @Immutable
    public enum class TipCode {
        ReviewDeadline;

        @Composable
        public fun tipText(): String = when (this) {
            ReviewDeadline -> AppTokens.strings.res(Res.string.goal_details_tip_review_deadline)
        }
    }
}
