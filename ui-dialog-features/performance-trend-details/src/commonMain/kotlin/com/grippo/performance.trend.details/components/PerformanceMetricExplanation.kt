package com.grippo.performance.trend.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.ArrowDown
import com.grippo.design.resources.provider.icons.ArrowTop
import com.grippo.design.resources.provider.performance_history_scope
import com.grippo.design.resources.provider.performance_trend_details_about
import com.grippo.design.resources.provider.performance_trend_details_collapsed
import com.grippo.design.resources.provider.performance_trend_details_expanded

private const val DisclosureAnimationDurationMillis = 200

@Composable
internal fun PerformanceMetricExplanation(
    modifier: Modifier = Modifier,
    metricType: PerformanceMetricTypeState,
    reference: PerformanceReferenceState,
    showChartScope: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    val expansionDescription = AppTokens.strings.res(
        if (isExpanded) {
            Res.string.performance_trend_details_expanded
        } else {
            Res.string.performance_trend_details_collapsed
        },
    )

    Column(modifier = modifier) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = expansionDescription },
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.performance_trend_details_about),
                endIcon = ButtonIcon.Icon(
                    value = if (isExpanded) AppTokens.icons.ArrowTop else AppTokens.icons.ArrowDown,
                ),
            ),
            style = ButtonStyle.Tertiary,
            onClick = onToggle,
        )
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(DisclosureAnimationDurationMillis),
                expandFrom = Alignment.Top,
            ) + fadeIn(animationSpec = tween(DisclosureAnimationDurationMillis)),
            exit = shrinkVertically(
                animationSpec = tween(DisclosureAnimationDurationMillis),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(animationSpec = tween(DisclosureAnimationDurationMillis)),
        ) {
            Column(
                modifier = Modifier.padding(top = AppTokens.dp.contentPadding.content),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            ) {
                Text(
                    text = metricType.description(),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
                if (showChartScope) {
                    Text(
                        text = AppTokens.strings.res(Res.string.performance_history_scope),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
                Text(
                    text = reference.comparisonScope(),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun PerformanceMetricExplanationCollapsedPreview() {
    PreviewContainer {
        PerformanceMetricExplanation(
            metricType = PerformanceMetricTypeState.Volume,
            reference = PerformanceReferenceState.Average,
            showChartScope = true,
            isExpanded = false,
            onToggle = {},
        )
    }
}

@AppPreview
@Composable
private fun PerformanceMetricExplanationExpandedPreview() {
    PreviewContainer {
        PerformanceMetricExplanation(
            metricType = PerformanceMetricTypeState.Volume,
            reference = PerformanceReferenceState.Average,
            showChartScope = true,
            isExpanded = true,
            onToggle = {},
        )
    }
}
