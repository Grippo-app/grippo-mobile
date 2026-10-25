package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.performance_baseline_average
import com.grippo.design.resources.provider.performance_baseline_previous
import com.grippo.design.resources.provider.training_recap_compared
import com.grippo.design.resources.provider.training_recap_compared_previous
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class PerformanceBaselineState {
    Average,

    Previous;

    public fun labelText(): UiText = when (this) {
        Average -> UiText.Res(Res.string.performance_baseline_average)
        Previous -> UiText.Res(Res.string.performance_baseline_previous)
    }

    @Composable
    public fun comparedSummary(improved: Int, compared: Int): String = when (this) {
        Average -> AppTokens.strings.res(Res.plurals.training_recap_compared, compared, improved, compared)
        Previous -> AppTokens.strings.res(Res.plurals.training_recap_compared_previous, compared, improved, compared)
    }
}
