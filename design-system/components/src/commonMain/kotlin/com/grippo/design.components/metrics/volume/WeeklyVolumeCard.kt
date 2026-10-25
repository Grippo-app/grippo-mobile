package com.grippo.design.components.metrics.volume

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.volume.WeeklyVolumeState
import com.grippo.core.state.metrics.volume.stubWeeklyVolume
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.skeleton.SkeletonPanel
import com.grippo.design.components.skeleton.SkeletonPanelStyle
import com.grippo.design.components.utils.ValueSlot
import com.grippo.design.components.utils.buildValueSlotString
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.weekly_volume_no_typical
import com.grippo.design.resources.provider.weekly_volume_progress
import com.grippo.design.resources.provider.weekly_volume_title
import com.grippo.design.resources.provider.weekly_volume_trainings

@Composable
public fun WeeklyVolumeCard(
    modifier: Modifier = Modifier,
    value: WeeklyVolumeState,
) {
    val typical = value.typicalVolume?.value
    val progress = value.progress()
    val currentText = VolumeFormatState.shortTotal(value.currentVolume.value)

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = AppTokens.strings.res(Res.string.weekly_volume_title),
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = value.weekRange.short(),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.End,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
            if (typical != null) {
                Text(
                    text = weeklyProgressText(
                        current = currentText,
                        typical = VolumeFormatState.shortTotal(typical),
                    ),
                    style = AppTokens.typography.h3(),
                    color = AppTokens.colors.text.primary,
                )
            } else {
                Text(
                    text = currentText,
                    style = AppTokens.typography.h3(),
                    color = AppTokens.colors.text.primary,
                )
                Text(
                    text = AppTokens.strings.res(Res.string.weekly_volume_no_typical),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }

        if (progress != null) {
            LineIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = progress,
                colors = if (progress >= 1f) {
                    AppTokens.colors.charts.indicator.success
                } else {
                    AppTokens.colors.charts.indicator.muted
                },
            )
        }

        Text(
            text = AppTokens.strings.res(Res.string.weekly_volume_trainings, value.trainingsThisWeek),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun weeklyProgressText(current: String, typical: String): AnnotatedString {
    val sentence = AppTokens.strings.res(Res.string.weekly_volume_progress, ValueSlot.marker(0), typical)
    val referenceStyle = AppTokens.typography.b12Med().toSpanStyle().copy(color = AppTokens.colors.text.secondary)

    return buildValueSlotString(
        sentence = sentence,
        slots = listOf(ValueSlot(value = current)),
        appendRest = { rest -> withStyle(referenceStyle) { append(rest) } },
    )
}

@Composable
public fun WeeklyVolumeCardSkeleton(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val titleLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val captionLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)
    val sentenceLine = rememberTextHeight(style = AppTokens.typography.h3(), textMeasurer = textMeasurer)
    val spacer = AppTokens.dp.metrics.panel.large.spacer

    SkeletonPanel(
        modifier = modifier,
        contentHeight = maxOf(titleLine, captionLine) + sentenceLine +
            AppTokens.dp.metrics.charts.indicator.height + captionLine + spacer * 3,
        style = SkeletonPanelStyle.Large,
    )
}

@AppPreview
@Composable
private fun WeeklyVolumeCardSkeletonPreview() {
    PreviewContainer {
        WeeklyVolumeCardSkeleton(modifier = Modifier.fillMaxWidth())
    }
}

@AppPreview
@Composable
private fun WeeklyVolumeCardBelowTypicalPreview() {
    PreviewContainer {
        WeeklyVolumeCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubWeeklyVolume(),
        )
    }
}

@AppPreview
@Composable
private fun WeeklyVolumeCardAboveTypicalPreview() {
    PreviewContainer {
        WeeklyVolumeCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubWeeklyVolume().copy(
                currentVolume = VolumeFormatState.ofTotal(11_250f),
                trainingsThisWeek = 4,
            ),
        )
    }
}

@AppPreview
@Composable
private fun WeeklyVolumeCardNoTypicalPreview() {
    PreviewContainer {
        WeeklyVolumeCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubWeeklyVolume().copy(
                currentVolume = VolumeFormatState.ofTotal(3_120f),
                trainingsThisWeek = 1,
                typicalVolume = null,
            ),
        )
    }
}

@AppPreview
@Composable
private fun WeeklyVolumeCardZeroTypicalPreview() {
    PreviewContainer {
        WeeklyVolumeCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubWeeklyVolume().copy(
                currentVolume = VolumeFormatState.ofTotal(0f),
                trainingsThisWeek = 2,
                typicalVolume = VolumeFormatState.ofTotal(0f),
            ),
        )
    }
}

@AppPreview
@Composable
private fun WeeklyVolumeCardZeroPreview() {
    PreviewContainer {
        WeeklyVolumeCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubWeeklyVolume().copy(
                currentVolume = VolumeFormatState.ofTotal(0f),
                trainingsThisWeek = 0,
            ),
        )
    }
}
