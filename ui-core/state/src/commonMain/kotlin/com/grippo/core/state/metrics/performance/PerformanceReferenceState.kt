package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.performance_previous_value
import com.grippo.design.resources.provider.performance_reference_average
import com.grippo.design.resources.provider.performance_reference_previous
import com.grippo.design.resources.provider.performance_trend_details_comparison_scope
import com.grippo.design.resources.provider.performance_trend_details_comparison_scope_previous
import com.grippo.design.resources.provider.performance_trend_details_first_workout
import com.grippo.design.resources.provider.performance_trend_details_no_previous
import com.grippo.design.resources.provider.performance_trend_details_previous_average
import com.grippo.design.resources.provider.performance_trend_details_zero_average
import com.grippo.design.resources.provider.performance_trend_details_zero_previous
import com.grippo.design.resources.provider.training_recap_compared
import com.grippo.design.resources.provider.training_recap_compared_previous
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class PerformanceReferenceState {
    Average,
    Previous;

    public fun labelText(): UiText = when (this) {
        Average -> UiText.Res(Res.string.performance_reference_average)
        Previous -> UiText.Res(Res.string.performance_reference_previous)
    }

    @Composable
    public fun comparedSummary(improved: Int, compared: Int): String = when (this) {
        Average -> AppTokens.strings.res(Res.plurals.training_recap_compared, compared, improved, compared)
        Previous -> AppTokens.strings.res(Res.plurals.training_recap_compared_previous, compared, improved, compared)
    }

    @Composable
    public fun detailedValue(value: String): String = when (this) {
        Average -> AppTokens.strings.res(Res.string.performance_trend_details_previous_average, value)
        Previous -> AppTokens.strings.res(Res.string.performance_previous_value, value)
    }

    @Composable
    public fun zeroValueNotice(): String = when (this) {
        Average -> AppTokens.strings.res(Res.string.performance_trend_details_zero_average)
        Previous -> AppTokens.strings.res(Res.string.performance_trend_details_zero_previous)
    }

    @Composable
    public fun missingValueNotice(): String = when (this) {
        Average -> AppTokens.strings.res(Res.string.performance_trend_details_first_workout)
        Previous -> AppTokens.strings.res(Res.string.performance_trend_details_no_previous)
    }

    @Composable
    public fun comparisonScope(): String = when (this) {
        Average -> AppTokens.strings.res(Res.string.performance_trend_details_comparison_scope)
        Previous -> AppTokens.strings.res(Res.string.performance_trend_details_comparison_scope_previous)
    }
}
