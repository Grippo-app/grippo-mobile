package com.grippo.design.components.metrics.distribution.muscle.loading.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import com.grippo.core.state.metrics.distribution.MuscleLoadEntryState
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.muscle.MuscleMark
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppColor

@Composable
internal fun MuscleLoadingItem(
    modifier: Modifier = Modifier,
    entry: MuscleLoadEntryState,
    label: String,
    color: Color,
    muscle: MuscleEnumState? = null,
    dominant: Boolean = false,
) {
    val progress = (entry.value / 100f).coerceIn(0f, 1f)

    val labelStyle =
        if (dominant) AppTokens.typography.h6()
        else AppTokens.typography.b13Med()

    val valueStyle =
        if (dominant) AppTokens.typography.b15Med()
        else AppTokens.typography.b12Med()

    val textColor =
        if (dominant) AppTokens.colors.text.primary
        else AppTokens.colors.text.secondary

    val valueText = PercentageFormatState.shortShare(entry.value)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (muscle == null) {
            MuscleMark(group = entry.group, color = color)
        } else {
            MuscleMark(muscle = muscle, color = color)
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = label,
                    style = labelStyle,
                    color = textColor,
                )
                Text(
                    text = valueText,
                    style = valueStyle,
                    color = textColor,
                    maxLines = 1,
                )
            }
            LineIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = progress,
                colors = rememberMuscleLoadIndicatorColors(color),
            )
        }
    }
}

@Composable
internal fun rememberMuscleLoadIndicatorColors(
    color: Color,
): AppColor.Charts.IndicatorColors.IndicatorColors {
    val trackColor = AppTokens.colors.charts.indicator.muted.track
    return remember(color, trackColor) {
        object : AppColor.Charts.IndicatorColors.IndicatorColors {
            override val colors = listOf(color)
            override val track = trackColor
        }
    }
}

@AppPreview
@Composable
private fun MuscleLoadingItemPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        val entry = summary.perGroup.entries.first()

        MuscleLoadingItem(
            entry = entry,
            label = entry.group.title().text(),
            color = summary.perGroup.color(entry),
            dominant = true,
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingItemLongLabelPreview() {
    PreviewContainer {
        val summary = stubMuscleLoadSummary()
        val entry = summary.perGroup.entries.first()
        MuscleLoadingItem(
            entry = entry,
            label = "Pectoralis major, sternocostal and clavicular portions",
            color = summary.perGroup.color(entry),
        )
    }
}
