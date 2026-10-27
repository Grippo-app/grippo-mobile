package com.grippo.design.components.metrics.volume

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.volume.WeeklyVolumeState
import com.grippo.core.state.metrics.volume.stubWeeklyVolume
import com.grippo.design.components.indicators.ComparisonIndicator
import com.grippo.design.components.skeleton.SkeletonBone
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
import com.grippo.design.resources.provider.weekly_volume_usual

private const val WEEKLY_VOLUME_REVEAL_DURATION_MS = 900
private const val WEEKLY_VOLUME_REVEAL_LABEL = "WeeklyVolumeReveal"

@Composable
public fun WeeklyVolumeSection(
    modifier: Modifier = Modifier,
    value: WeeklyVolumeState,
) {
    val typical = value.typicalVolume?.value
    val usualVolume = value.usualVolume()
    val current = value.currentVolume.value ?: 0f
    val progress = value.progress()
    val currentText = VolumeFormatState.shortTotal(value.currentVolume.value)
    val scale = maxOf(current, typical ?: 0f)
    val currentFraction = if (scale > 0f) current / scale else 0f
    val typicalFraction = if (typical != null && scale > 0f) typical / scale else 0f

    var revealed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val animatedCurrentFraction = animateFloatAsState(
        targetValue = if (revealed) currentFraction else typicalFraction,
        animationSpec = tween(durationMillis = WEEKLY_VOLUME_REVEAL_DURATION_MS, easing = FastOutSlowInEasing),
        label = WEEKLY_VOLUME_REVEAL_LABEL,
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.metrics.section.spacer),
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
                text = value.weekRange.shortStartEnd(),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.End,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
            when {
                current > 0f && usualVolume != null -> Text(
                    text = weeklyProgressText(
                        current = currentText,
                        typical = VolumeFormatState.shortTotal(usualVolume),
                    ),
                    style = AppTokens.typography.h3(),
                    color = AppTokens.colors.text.primary,
                )

                current > 0f -> {
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

                usualVolume != null -> Text(
                    text = AppTokens.strings.res(
                        Res.string.weekly_volume_usual,
                        VolumeFormatState.shortTotal(usualVolume),
                    ),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )

                else -> Text(
                    text = AppTokens.strings.res(Res.string.weekly_volume_no_typical),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }

        if (progress != null) {
            ComparisonIndicator(
                modifier = Modifier.fillMaxWidth(),
                value = { animatedCurrentFraction.value },
                reference = { typicalFraction },
                colors = if (progress >= 1f) {
                    AppTokens.colors.charts.indicator.success
                } else {
                    AppTokens.colors.charts.indicator.muted
                },
            )
        }

        Text(
            text = value.workoutsLabel(),
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
public fun WeeklyVolumeSectionSkeleton(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val titleLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val captionLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)
    val sentenceLine = rememberTextHeight(style = AppTokens.typography.h3(), textMeasurer = textMeasurer)
    val indicatorHeight = maxOf(
        AppTokens.dp.metrics.charts.comparison.height,
        AppTokens.dp.metrics.charts.comparison.marker.height,
    )
    val spacer = AppTokens.dp.metrics.section.spacer

    SkeletonBone(
        modifier = modifier
            .fillMaxWidth()
            .height(maxOf(titleLine, captionLine) + sentenceLine + indicatorHeight + captionLine + spacer * 3),
    )
}

@AppPreview
@Composable
private fun WeeklyVolumeSectionSkeletonPreview() {
    PreviewContainer {
        WeeklyVolumeSectionSkeleton(modifier = Modifier.fillMaxWidth())
    }
}

@AppPreview
@Composable
private fun WeeklyVolumeSectionBelowTypicalPreview() {
    PreviewContainer {
        WeeklyVolumeSection(
            modifier = Modifier.fillMaxWidth(),
            value = stubWeeklyVolume(),
        )
    }
}

@AppPreview
@Composable
private fun WeeklyVolumeSectionAboveTypicalPreview() {
    PreviewContainer {
        WeeklyVolumeSection(
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
private fun WeeklyVolumeSectionNoTypicalPreview() {
    PreviewContainer {
        WeeklyVolumeSection(
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
private fun WeeklyVolumeSectionZeroTypicalPreview() {
    PreviewContainer {
        WeeklyVolumeSection(
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
private fun WeeklyVolumeSectionZeroPreview() {
    PreviewContainer {
        WeeklyVolumeSection(
            modifier = Modifier.fillMaxWidth(),
            value = stubWeeklyVolume().copy(
                currentVolume = VolumeFormatState.ofTotal(0f),
                trainingsThisWeek = 0,
            ),
        )
    }
}
