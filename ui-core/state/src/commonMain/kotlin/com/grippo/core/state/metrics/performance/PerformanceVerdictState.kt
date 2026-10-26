package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training.recapDeclinedBackdrop
import com.grippo.design.resources.provider.training.recapFirstWorkoutBackdrop
import com.grippo.design.resources.provider.training.recapImprovedBackdrop
import com.grippo.design.resources.provider.training.recapRecordBackdrop
import com.grippo.design.resources.provider.training.recapStableBackdrop
import com.grippo.design.resources.provider.training_recap_first_description
import com.grippo.design.resources.provider.training_recap_verdict_declined
import com.grippo.design.resources.provider.training_recap_verdict_first
import com.grippo.design.resources.provider.training_recap_verdict_improved
import com.grippo.design.resources.provider.training_recap_verdict_improved_previous
import com.grippo.design.resources.provider.training_recap_verdict_record
import com.grippo.design.resources.provider.training_recap_verdict_stable
import kotlinx.collections.immutable.ImmutableList

@Immutable
public enum class PerformanceVerdictState {
    Record,
    Improved,
    Stable,
    Declined,
    FirstWorkout;

    @Composable
    public fun title(reference: PerformanceReferenceState): String = when (this) {
        Record -> AppTokens.strings.res(Res.string.training_recap_verdict_record)

        Improved -> AppTokens.strings.res(
            when (reference) {
                PerformanceReferenceState.Average -> Res.string.training_recap_verdict_improved
                PerformanceReferenceState.Previous -> Res.string.training_recap_verdict_improved_previous
            },
        )

        Stable -> AppTokens.strings.res(Res.string.training_recap_verdict_stable)
        Declined -> AppTokens.strings.res(Res.string.training_recap_verdict_declined)
        FirstWorkout -> AppTokens.strings.res(Res.string.training_recap_verdict_first)
    }

    @Composable
    public fun rememberBackdrop(): ImageVector {
        val accent = AppTokens.colors.illustration.accent

        return remember(this, accent) {
            when (this) {
                Record -> recapRecordBackdrop(accent)
                Improved -> recapImprovedBackdrop(accent)
                Stable -> recapStableBackdrop(accent)
                Declined -> recapDeclinedBackdrop(accent)
                FirstWorkout -> recapFirstWorkoutBackdrop(accent)
            }
        }
    }

    @Composable
    public fun summary(
        metrics: ImmutableList<PerformanceMetricState>,
        reference: PerformanceReferenceState,
    ): String = when (this) {
        FirstWorkout -> AppTokens.strings.res(Res.string.training_recap_first_description)
        Record, Improved, Stable, Declined -> reference.comparedSummary(
            improved = improvedCount(metrics),
            compared = comparedCount(metrics),
        )
    }

    public companion object {
        public fun of(metrics: List<PerformanceMetricState>): PerformanceVerdictState {
            val compared = metrics.filter { it.showsComparison() }
            if (compared.isEmpty()) {
                return FirstWorkout
            }

            if (compared.any { it.status == PerformanceTrendStatusState.Record }) {
                return Record
            }

            val improved = compared.count { it.status == PerformanceTrendStatusState.Improved }
            val declined = compared.count { it.status == PerformanceTrendStatusState.Declined }
            return when {
                improved > declined -> Improved
                declined > improved -> Declined
                else -> Stable
            }
        }

        private fun improvedCount(metrics: List<PerformanceMetricState>): Int = metrics.count { metric ->
            metric.showsComparison() && (
                metric.status == PerformanceTrendStatusState.Record ||
                    metric.status == PerformanceTrendStatusState.Improved
                )
        }

        private fun comparedCount(metrics: List<PerformanceMetricState>): Int = metrics.count { it.showsComparison() }
    }
}
