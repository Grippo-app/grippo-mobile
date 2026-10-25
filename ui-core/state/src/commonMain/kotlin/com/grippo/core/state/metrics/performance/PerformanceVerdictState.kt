package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.ExperienceIntermediate
import com.grippo.design.resources.provider.icons.LineDown
import com.grippo.design.resources.provider.icons.LineFlat
import com.grippo.design.resources.provider.icons.LineUp
import com.grippo.design.resources.provider.icons.Sparkle
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
    public fun title(baseline: PerformanceBaselineState): String = when (this) {
        Record -> AppTokens.strings.res(Res.string.training_recap_verdict_record)

        Improved -> AppTokens.strings.res(
            when (baseline) {
                PerformanceBaselineState.Average -> Res.string.training_recap_verdict_improved
                PerformanceBaselineState.Previous -> Res.string.training_recap_verdict_improved_previous
            },
        )

        Stable -> AppTokens.strings.res(Res.string.training_recap_verdict_stable)
        Declined -> AppTokens.strings.res(Res.string.training_recap_verdict_declined)
        FirstWorkout -> AppTokens.strings.res(Res.string.training_recap_verdict_first)
    }

    @Composable
    public fun icon(): ImageVector = when (this) {
        Record -> AppTokens.icons.ExperienceIntermediate
        Improved -> AppTokens.icons.LineUp
        Stable -> AppTokens.icons.LineFlat
        Declined -> AppTokens.icons.LineDown
        FirstWorkout -> AppTokens.icons.Sparkle
    }

    @Composable
    public fun color(): Color = when (this) {
        Record, Improved -> AppTokens.colors.semantic.success
        Declined -> AppTokens.colors.semantic.decline
        Stable, FirstWorkout -> AppTokens.colors.text.secondary
    }

    @Composable
    public fun summary(
        metrics: ImmutableList<PerformanceMetricState>,
        baseline: PerformanceBaselineState,
    ): String = when (this) {
        FirstWorkout -> AppTokens.strings.res(Res.string.training_recap_first_description)
        Record, Improved, Stable, Declined -> baseline.comparedSummary(
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

        public fun improvedCount(metrics: List<PerformanceMetricState>): Int = metrics.count { metric ->
            metric.showsComparison() && (
                metric.status == PerformanceTrendStatusState.Record ||
                    metric.status == PerformanceTrendStatusState.Improved
                )
        }

        public fun comparedCount(metrics: List<PerformanceMetricState>): Int = metrics.count { it.showsComparison() }
    }
}
