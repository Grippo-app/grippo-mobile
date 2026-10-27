package com.grippo.exercise.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.text.rememberTextMeasurer
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.FormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.banner.BannerCard
import com.grippo.design.components.banner.BannerCardStyle
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.metrics.volume.ExerciseVolumeHistoryChart
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_history_chart_unavailable
import com.grippo.design.resources.provider.exercise_history_comparison_loading
import com.grippo.design.resources.provider.exercise_history_max_load
import com.grippo.design.resources.provider.exercise_history_no_previous
import com.grippo.design.resources.provider.exercise_history_total_volume
import com.grippo.design.resources.provider.exercise_history_trend_title
import com.grippo.design.resources.provider.exercise_history_volume_delta
import com.grippo.design.resources.provider.icons.LineUp
import com.grippo.design.resources.provider.joined_pair
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.percent
import com.grippo.design.resources.provider.repetitions
import com.grippo.exercise.ExerciseState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime
import kotlin.math.roundToInt

@Composable
internal fun ExerciseProgressCard(
    modifier: Modifier = Modifier,
    value: ExerciseState.Available,
    isHistoryLoading: Boolean = false,
) {
    val exercise = value.exercise
    val zeroVolume = "0${FormatState.UNIT_SPACE}${AppTokens.strings.res(Res.string.kg)}"
    val repetitions = exercise.total.repetitions.value ?: 0
    val maximum = exercise.maximumRecordedLoad()
    val maximumVolume = VolumeFormatState.of(maximum)

    ExerciseProgressPanel(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            ResultMetric(
                modifier = Modifier.weight(1f),
                label = AppTokens.strings.res(Res.string.exercise_history_total_volume),
                current = if (exercise.total.volume.value == null) zeroVolume else exercise.total.volume.short(),
            )
            ResultMetric(
                modifier = Modifier.weight(1f),
                label = AppTokens.strings.res(Res.string.repetitions),
                current = repetitions.toString(),
            )
            ResultMetric(
                modifier = Modifier.weight(1f),
                label = AppTokens.strings.res(Res.string.exercise_history_max_load),
                current = when {
                    maximum == null -> "—"
                    maximumVolume.value == null -> zeroVolume
                    else -> maximumVolume.short()
                },
            )
        }

        ContentDivider()

        ExerciseVolumeHistory(value = value, isHistoryLoading = isHistoryLoading)
    }
}

@Composable
internal fun ExerciseProgressCardSkeleton(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val valueLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val labelLine = rememberTextHeight(style = AppTokens.typography.b11Med(), textMeasurer = textMeasurer)
    val captionStyle = AppTokens.typography.b12Med()

    ExerciseProgressPanel(modifier = modifier) {
        SkeletonBone(
            modifier = Modifier
                .fillMaxWidth()
                .height(valueLine + AppTokens.dp.contentPadding.text + labelLine),
        )

        ContentDivider()

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val chartHeight = maxWidth / AppTokens.dp.metrics.volume.chart.historyAspectRatio

            Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.short),
                    style = captionStyle,
                    textMeasurer = textMeasurer,
                )
                ExerciseHistorySkeleton(
                    captionStyle = captionStyle,
                    chartHeight = chartHeight,
                    textMeasurer = textMeasurer,
                )
            }
        }
    }
}

@Composable
private fun ExerciseProgressPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val panel = AppTokens.dp.metrics.panel.large
    val shape = RoundedCornerShape(panel.radius)

    Column(
        modifier = modifier
            .background(color = AppTokens.colors.background.card, shape = shape)
            .border(
                width = AppTokens.dp.metrics.panel.borderWidth,
                color = AppTokens.colors.border.default,
                shape = shape,
            )
            .padding(horizontal = panel.horizontalPadding, vertical = panel.verticalPadding),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        content = content,
    )
}

@Composable
private fun ResultMetric(
    modifier: Modifier = Modifier,
    label: String,
    current: String,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(text = current, style = AppTokens.typography.h5(), color = AppTokens.colors.text.primary)
        Text(text = label, style = AppTokens.typography.b11Med(), color = AppTokens.colors.text.secondary)
    }
}

@Composable
private fun ExerciseVolumeHistory(
    modifier: Modifier = Modifier,
    value: ExerciseState.Available,
    isHistoryLoading: Boolean,
) {
    val exercise = value.exercise
    val records = value.history
    val previous = if (isHistoryLoading) null else records.lastOrNull()
    val volume = exercise.total.volume.value ?: 0f
    val chartValuesSupported = volume.isFinite() && volume >= 0f && records.all {
        val recordedVolume = it.total.volume.value ?: 0f
        recordedVolume.isFinite() && recordedVolume >= 0f
    }
    val previousVolume = previous?.total?.volume?.value ?: 0f
    val percent = if (previousVolume > 0f && volume >= 0f) {
        ((volume.toDouble() - previousVolume) / previousVolume * 100.0)
            .takeIf { it.isFinite() && it <= Int.MAX_VALUE }
            ?.roundToInt()
    } else {
        null
    }
    val percentText = percent?.let {
        val formatted = PercentageFormatState.of(it)
        when {
            it > 0 -> "+${formatted.short()}"
            it == 0 -> "0${AppTokens.strings.res(Res.string.percent)}"
            else -> formatted.short()
        }
    }
    val percentDescription = percentText?.let {
        AppTokens.strings.res(Res.string.exercise_history_volume_delta, it)
    }
    val previousDate = previous?.createdAt?.value?.let {
        DateTimeUtils.format(value = it, format = DateFormat.DateOnly.DateDdMmm)
    } ?: previous?.createdAt?.display
    val caption = previous?.let {
        val previousVolumeText = if (it.total.volume.value == null) {
            "0${FormatState.UNIT_SPACE}${AppTokens.strings.res(Res.string.kg)}"
        } else {
            it.total.volume.short()
        }
        AppTokens.strings.res(
            Res.string.joined_pair,
            previousVolumeText,
            previousDate.orEmpty(),
        )
    }
    val captionStyle = AppTokens.typography.b12Med()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val chartHeight = maxWidth / AppTokens.dp.metrics.volume.chart.historyAspectRatio

        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = AppTokens.strings.res(Res.string.exercise_history_trend_title),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
                if (percentText != null) {
                    Text(
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = percentDescription.orEmpty()
                        },
                        text = percentText,
                        style = AppTokens.typography.b12Semi(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }

            if (isHistoryLoading) {
                val loadingDescription = AppTokens.strings.res(Res.string.exercise_history_comparison_loading)

                ExerciseHistorySkeleton(
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = loadingDescription
                        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                    },
                    captionStyle = captionStyle,
                    chartHeight = chartHeight,
                )
            } else {
                val captionMinHeight = rememberTextHeight(style = captionStyle, lines = 2)

                Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = captionMinHeight),
                    ) {
                        if (caption != null) {
                            Text(
                                text = caption,
                                style = captionStyle,
                                color = AppTokens.colors.text.secondary,
                                minLines = 2,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = chartHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (previous != null && chartValuesSupported) {
                            ExerciseVolumeHistoryChart(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(chartHeight),
                                value = value.history.toPersistentList().adding(exercise),
                            )
                        } else {
                            val shape = RoundedCornerShape(AppTokens.dp.metrics.panel.small.radius)
                            BannerCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = AppTokens.colors.background.dialog, shape = shape)
                                    .padding(AppTokens.dp.contentPadding.subContent),
                                style = BannerCardStyle.Custom(
                                    tint = AppTokens.colors.icon.secondary,
                                    surface = AppTokens.colors.background.card,
                                ),
                                icon = AppTokens.icons.LineUp,
                                title = AppTokens.strings.res(
                                    when {
                                        previous == null -> Res.string.exercise_history_no_previous
                                        else -> Res.string.exercise_history_chart_unavailable
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ExerciseProgressCardHistoryPreview() {
    PreviewContainer {
        ExerciseProgressCard(value = progressPreviewState(historyCount = 5))
    }
}

@AppPreview
@Composable
private fun ExerciseProgressCardWithoutHistoryPreview() {
    PreviewContainer {
        ExerciseProgressCard(value = progressPreviewState(historyCount = 0))
    }
}

@AppPreview
@Composable
private fun ExerciseProgressCardChangedAssistancePreview() {
    val value = progressPreviewState(historyCount = 1)
    val current = value.exercise.copy(
        iterations = value.exercise.iterations.map {
            it.copy(
                externalWeight = VolumeFormatState.Empty(),
                bodyWeight = WeightFormatState.of(80f),
                bodyMultiplier = MultiplierFormatState.of(1f),
            )
        }.toPersistentList(),
    )
    val previous = value.history.last()
    val assisted = previous.copy(
        iterations = current.iterations.map {
            it.copy(assistWeight = VolumeFormatState.of(20f), repetitions = RepetitionsFormatState.of(5))
        }.toPersistentList(),
        total = previous.total.copy(volume = VolumeFormatState.of(900f), intensity = IntensityFormatState.of(60f)),
    )
    PreviewContainer {
        ExerciseProgressCard(value = value.copy(exercise = current, history = persistentListOf(assisted)))
    }
}

@AppPreview
@Composable
private fun ExerciseProgressCardPartialMaximumPreview() {
    val value = progressPreviewState(historyCount = 1)
    val iterations = value.exercise.iterations.toPersistentList().replacingAt(
        index = 1,
        element = stubPendingIteration().copy(
            id = "incomplete-load",
            bodyWeight = WeightFormatState.of(80f),
            repetitions = RepetitionsFormatState.of(10),
        ),
    )
    PreviewContainer {
        ExerciseProgressCard(value = value.copy(exercise = value.exercise.copy(iterations = iterations)))
    }
}

@AppPreview
@Composable
private fun ExerciseProgressCardLargeIncreasePreview() {
    val value = progressPreviewState(historyCount = 1)
    val previous = value.history.last()
    val light = previous.copy(
        iterations = previous.iterations.map { it.copy(externalWeight = VolumeFormatState.of(2f)) }.toPersistentList(),
        total = previous.total.copy(volume = VolumeFormatState.of(30f), intensity = IntensityFormatState.of(2f)),
    )
    PreviewContainer {
        ExerciseProgressCard(value = value.copy(history = persistentListOf(light)))
    }
}

@AppPreview
@Composable
private fun ExerciseProgressCardLoadingPreview() {
    val value = progressPreviewState(historyCount = 0)
    PreviewContainer {
        ExerciseProgressCard(value = value, isHistoryLoading = true)
    }
}

@AppPreview
@Composable
private fun ExerciseProgressCardSkeletonPreview() {
    PreviewContainer {
        Column(
            modifier = Modifier
                .background(AppTokens.colors.background.dialog)
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        ) {
            ExerciseProgressCardSkeleton(modifier = Modifier.fillMaxWidth())
        }
    }
}

@AppPreview
@Composable
private fun ExerciseProgressCardNegativeVolumePreview() {
    val value = progressPreviewState(historyCount = 1)
    val history = value.history
    val iterations = value.exercise.iterations.map { iteration ->
        iteration.copy(
            externalWeight = VolumeFormatState.Empty(),
            bodyWeight = WeightFormatState.of(80f),
            bodyMultiplier = MultiplierFormatState.of(1f),
            assistWeight = VolumeFormatState.of(90f),
        )
    }.toPersistentList()
    val exercise = value.exercise.copy(
        iterations = iterations,
        total = value.exercise.total.copy(
            volume = VolumeFormatState.of(-300f),
            intensity = IntensityFormatState.of(-10f),
        ),
    )
    val previous = history.last().copy(
        iterations = iterations.map {
            it.copy(
                assistWeight = VolumeFormatState.of(100f),
                repetitions = RepetitionsFormatState.of(5),
            )
        }.toPersistentList(),
        total = history.last().total.copy(
            volume = VolumeFormatState.of(-300f),
            intensity = IntensityFormatState.of(-20f),
        ),
    )
    PreviewContainer {
        ExerciseProgressCard(
            value = value.copy(
                exercise = exercise,
                history = persistentListOf(previous),
            ),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseProgressCardZeroBaselinePreview() {
    val value = progressPreviewState(historyCount = 1)
    val history = value.history
    val iterations = value.exercise.iterations.map { iteration ->
        iteration.copy(
            externalWeight = VolumeFormatState.Empty(),
            bodyWeight = WeightFormatState.of(80f),
            bodyMultiplier = MultiplierFormatState.of(1f),
            assistWeight = VolumeFormatState.of(10f),
        )
    }.toPersistentList()
    val exercise = value.exercise.copy(
        iterations = iterations,
        total = value.exercise.total.copy(
            volume = VolumeFormatState.of(2_100f),
            intensity = IntensityFormatState.of(70f),
        ),
    )
    val previous = history.last().copy(
        iterations = iterations.map { it.copy(assistWeight = VolumeFormatState.of(80f)) }.toPersistentList(),
        total = exercise.total.copy(volume = VolumeFormatState.of(0f), intensity = IntensityFormatState.of(0f)),
    )
    val records = persistentListOf(previous)
    PreviewContainer {
        ExerciseProgressCard(
            value = value.copy(exercise = exercise, history = records),
        )
    }
}

private fun progressPreviewState(historyCount: Int): ExerciseState.Available {
    val date = DateTimeFormatState.of(
        value = LocalDateTime(year = 2026, month = 10, day = 16, hour = 18, minute = 0),
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateDdMmmYyyy,
    )
    val iteration = stubPendingIteration().copy(
        id = "progress-set",
        externalWeight = VolumeFormatState.of(80f),
        repetitions = RepetitionsFormatState.of(10),
    )
    val exercise = stubExercise().copy(
        id = "progress-preview",
        exerciseExample = stubExercise().exerciseExample.copy(id = "progress-example", name = "Bench Press"),
        createdAt = date,
        iterations = (0..2).map { iteration.copy(id = "progress-set-$it") }.toPersistentList(),
        total = TrainingTotalState(
            volume = VolumeFormatState.of(2_400f),
            repetitions = RepetitionsFormatState.of(30),
            intensity = IntensityFormatState.of(80f),
        ),
    )
    val previous = (0 until historyCount).map { index ->
        val repetitions = 5 + index
        exercise.copy(
            id = "previous-$index",
            createdAt = DateTimeFormatState.of(
                value = LocalDateTime(
                    year = 2026,
                    month = 10,
                    day = index * 3 + 1,
                    hour = 18,
                    minute = 0,
                ),
                range = DateRangePresets.infinity(),
                format = DateFormat.DateOnly.DateDdMmmYyyy,
            ),
            iterations = (0..2).map { set ->
                iteration.copy(id = "previous-$index-$set", repetitions = RepetitionsFormatState.of(repetitions))
            }.toPersistentList(),
            total = exercise.total.copy(
                volume = VolumeFormatState.of(80f * repetitions * 3),
                repetitions = RepetitionsFormatState.of(repetitions * 3),
            ),
        )
    }.toPersistentList()
    return ExerciseState.Available(exercise = exercise, history = previous)
}
