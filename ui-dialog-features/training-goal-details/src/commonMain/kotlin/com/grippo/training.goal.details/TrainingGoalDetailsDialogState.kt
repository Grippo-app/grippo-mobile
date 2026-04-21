package com.grippo.training.goal.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.GoalProgressState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_details_reason_almost_done_detail
import com.grippo.design.resources.provider.goal_details_reason_almost_done_title
import com.grippo.design.resources.provider.goal_details_reason_balance_drift_detail
import com.grippo.design.resources.provider.goal_details_reason_balance_drift_title
import com.grippo.design.resources.provider.goal_details_reason_balance_ok_detail
import com.grippo.design.resources.provider.goal_details_reason_balance_ok_title
import com.grippo.design.resources.provider.goal_details_reason_completed_detail
import com.grippo.design.resources.provider.goal_details_reason_completed_title
import com.grippo.design.resources.provider.goal_details_reason_drifting_detail
import com.grippo.design.resources.provider.goal_details_reason_drifting_title
import com.grippo.design.resources.provider.goal_details_reason_endurance_low_detail
import com.grippo.design.resources.provider.goal_details_reason_endurance_low_title
import com.grippo.design.resources.provider.goal_details_reason_endurance_ok_detail
import com.grippo.design.resources.provider.goal_details_reason_endurance_ok_title
import com.grippo.design.resources.provider.goal_details_reason_hypertrophy_low_detail
import com.grippo.design.resources.provider.goal_details_reason_hypertrophy_low_title
import com.grippo.design.resources.provider.goal_details_reason_hypertrophy_ok_detail
import com.grippo.design.resources.provider.goal_details_reason_hypertrophy_ok_title
import com.grippo.design.resources.provider.goal_details_reason_no_data_detail
import com.grippo.design.resources.provider.goal_details_reason_no_data_title
import com.grippo.design.resources.provider.goal_details_reason_off_track_detail
import com.grippo.design.resources.provider.goal_details_reason_off_track_title
import com.grippo.design.resources.provider.goal_details_reason_on_track_detail
import com.grippo.design.resources.provider.goal_details_reason_on_track_title
import com.grippo.design.resources.provider.goal_details_reason_overdue_detail
import com.grippo.design.resources.provider.goal_details_reason_overdue_title
import com.grippo.design.resources.provider.goal_details_reason_rtt_in_range_detail
import com.grippo.design.resources.provider.goal_details_reason_rtt_in_range_title
import com.grippo.design.resources.provider.goal_details_reason_rtt_over_detail
import com.grippo.design.resources.provider.goal_details_reason_rtt_over_title
import com.grippo.design.resources.provider.goal_details_reason_rtt_under_detail
import com.grippo.design.resources.provider.goal_details_reason_rtt_under_title
import com.grippo.design.resources.provider.goal_details_reason_strength_low_detail
import com.grippo.design.resources.provider.goal_details_reason_strength_low_title
import com.grippo.design.resources.provider.goal_details_reason_strength_ok_detail
import com.grippo.design.resources.provider.goal_details_reason_strength_ok_title
import com.grippo.design.resources.provider.goal_details_reason_suppressed_axis_high_detail
import com.grippo.design.resources.provider.goal_details_reason_suppressed_axis_high_title
import com.grippo.design.resources.provider.goal_details_reason_too_early_detail
import com.grippo.design.resources.provider.goal_details_reason_too_early_title
import com.grippo.design.resources.provider.goal_details_tip_add_heavy
import com.grippo.design.resources.provider.goal_details_tip_balance_strength_end
import com.grippo.design.resources.provider.goal_details_tip_balance_strength_hyp
import com.grippo.design.resources.provider.goal_details_tip_consistent
import com.grippo.design.resources.provider.goal_details_tip_dial_back
import com.grippo.design.resources.provider.goal_details_tip_hypertrophy_reps
import com.grippo.design.resources.provider.goal_details_tip_metabolic
import com.grippo.design.resources.provider.goal_details_tip_ramp_up
import com.grippo.design.resources.provider.goal_details_tip_reduce_suppressed
import com.grippo.design.resources.provider.goal_details_tip_review_deadline
import com.grippo.design.resources.provider.goal_details_tip_stay_course
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class TrainingGoalDetailsDialogState(
    val range: DateRangeFormatState,
    val progress: GoalProgressState? = null,
    val insights: ImmutableList<InsightItem> = persistentListOf(),
    val tips: ImmutableList<TipCode> = persistentListOf(),
) {

    /**
     * A short diagnostic statement about the goal. Rendered as a row
     * with a colored leading dot, a headline and an explanation.
     */
    @Immutable
    public data class InsightItem(
        val severity: Severity,
        val reason: ReasonCode,
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

            @Composable
            public fun color(): Color = when (this) {
                Positive -> AppTokens.colors.semantic.success
                Warning -> AppTokens.colors.semantic.warning
                Negative -> AppTokens.colors.semantic.error
                Neutral -> AppTokens.colors.semantic.info
            }
        }
    }

    /**
     * Diagnostic reason codes. Each code is mapped to a localized
     * headline + detail string in the screen layer (via AppTokens.strings).
     */
    @Immutable
    public enum class ReasonCode {
        NoData,
        TooEarly,
        Overdue,
        AlmostDone,
        Completed,

        // Primary-goal-specific deviations
        StrengthShareLow,
        StrengthShareOk,
        HypertrophyShareLow,
        HypertrophyShareOk,
        EnduranceShareLow,
        EnduranceShareOk,
        BalanceDrift,
        BalanceOk,
        SuppressedAxisTooHigh,
        ReturnToTrainingUnderWork,
        ReturnToTrainingOverWork,
        ReturnToTrainingInRange,

        // Score-level summary reasons
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

            Completed -> AppTokens.strings.res(Res.string.goal_details_reason_completed_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_completed_detail)

            ScoreOnTrack -> AppTokens.strings.res(Res.string.goal_details_reason_on_track_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_on_track_detail)

            ScoreDrifting -> AppTokens.strings.res(Res.string.goal_details_reason_drifting_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_drifting_detail)

            ScoreOffTrack -> AppTokens.strings.res(Res.string.goal_details_reason_off_track_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_off_track_detail)

            StrengthShareLow -> AppTokens.strings.res(Res.string.goal_details_reason_strength_low_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_strength_low_detail)

            StrengthShareOk -> AppTokens.strings.res(Res.string.goal_details_reason_strength_ok_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_strength_ok_detail)

            HypertrophyShareLow -> AppTokens.strings.res(Res.string.goal_details_reason_hypertrophy_low_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_hypertrophy_low_detail)

            HypertrophyShareOk -> AppTokens.strings.res(Res.string.goal_details_reason_hypertrophy_ok_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_hypertrophy_ok_detail)

            EnduranceShareLow -> AppTokens.strings.res(Res.string.goal_details_reason_endurance_low_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_endurance_low_detail)

            EnduranceShareOk -> AppTokens.strings.res(Res.string.goal_details_reason_endurance_ok_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_endurance_ok_detail)

            BalanceDrift -> AppTokens.strings.res(Res.string.goal_details_reason_balance_drift_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_balance_drift_detail)

            BalanceOk -> AppTokens.strings.res(Res.string.goal_details_reason_balance_ok_title) to
                    AppTokens.strings.res(Res.string.goal_details_reason_balance_ok_detail)

            SuppressedAxisTooHigh ->
                AppTokens.strings.res(Res.string.goal_details_reason_suppressed_axis_high_title) to
                        AppTokens.strings.res(Res.string.goal_details_reason_suppressed_axis_high_detail)

            ReturnToTrainingUnderWork ->
                AppTokens.strings.res(Res.string.goal_details_reason_rtt_under_title) to
                        AppTokens.strings.res(Res.string.goal_details_reason_rtt_under_detail)

            ReturnToTrainingOverWork ->
                AppTokens.strings.res(Res.string.goal_details_reason_rtt_over_title) to
                        AppTokens.strings.res(Res.string.goal_details_reason_rtt_over_detail)

            ReturnToTrainingInRange ->
                AppTokens.strings.res(Res.string.goal_details_reason_rtt_in_range_title) to
                        AppTokens.strings.res(Res.string.goal_details_reason_rtt_in_range_detail)
        }
    }

    /**
     * Recommendations shown in TipCards. Each code resolves to a
     * concrete, actionable sentence in the screen layer.
     */
    @Immutable
    public enum class TipCode {
        AddHeavyCompounds,
        IncreaseHypertrophyReps,
        AddMetabolicWork,
        BalanceStrengthAndHypertrophy,
        BalanceStrengthAndEndurance,
        ReduceSuppressedAxis,
        RampUpGradually,
        DialBackIntensity,
        KeepConsistentFrequency,
        ReviewDeadline,
        StayTheCourse;

        @Composable
        public fun tipText(): String = when (this) {
            AddHeavyCompounds -> AppTokens.strings.res(Res.string.goal_details_tip_add_heavy)
            IncreaseHypertrophyReps -> AppTokens.strings.res(Res.string.goal_details_tip_hypertrophy_reps)
            AddMetabolicWork -> AppTokens.strings.res(Res.string.goal_details_tip_metabolic)
            BalanceStrengthAndHypertrophy -> AppTokens.strings.res(Res.string.goal_details_tip_balance_strength_hyp)
            BalanceStrengthAndEndurance -> AppTokens.strings.res(Res.string.goal_details_tip_balance_strength_end)
            ReduceSuppressedAxis -> AppTokens.strings.res(Res.string.goal_details_tip_reduce_suppressed)
            RampUpGradually -> AppTokens.strings.res(Res.string.goal_details_tip_ramp_up)
            DialBackIntensity -> AppTokens.strings.res(Res.string.goal_details_tip_dial_back)
            KeepConsistentFrequency -> AppTokens.strings.res(Res.string.goal_details_tip_consistent)
            ReviewDeadline -> AppTokens.strings.res(Res.string.goal_details_tip_review_deadline)
            StayTheCourse -> AppTokens.strings.res(Res.string.goal_details_tip_stay_course)
        }
    }
}
