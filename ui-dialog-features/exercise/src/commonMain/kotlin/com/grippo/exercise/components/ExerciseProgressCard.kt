package com.grippo.exercise.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.MultiplierFormatState
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
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_history_chart_unavailable
import com.grippo.design.resources.provider.exercise_history_different_load
import com.grippo.design.resources.provider.exercise_history_max_load
import com.grippo.design.resources.provider.exercise_history_no_previous
import com.grippo.design.resources.provider.exercise_history_previous_short
import com.grippo.design.resources.provider.exercise_history_total_volume
import com.grippo.design.resources.provider.exercise_history_trend_title
import com.grippo.design.resources.provider.exercise_history_volume_delta
import com.grippo.design.resources.provider.repetitions
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.icons.LineUp
import com.grippo.exercise.ExerciseState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ExerciseProgressCard(
    modifier: Modifier = Modifier,
    value: ExerciseState.Available,
    isHistoryLoading: Boolean = false,
) {
    val exercise = value.exercise
    // Stored totals originate in non-null domain fields; formatter Empty means recorded zero.
    val zeroVolume = "0${AppTokens.strings.res(Res.string.kg)}"
    val repetitions = exercise.total.repetitions.value ?: 0
    val maximum = exercise.maximumRecordedLoad()
    val maximumVolume = VolumeFormatState.of(maximum)
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
    ) {
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
    val comparable = previous != null && exercise.hasComparableLoadTo(previous)
    val chartComparable = previous != null && records.all { exercise.hasComparableLoadTo(it) }
    val volume = exercise.total.volume.value ?: 0f
    val chartValuesSupported = volume.isFinite() && volume >= 0f && records.all {
        val recordedVolume = it.total.volume.value ?: 0f
        recordedVolume.isFinite() && recordedVolume >= 0f
    }
    val previousVolume = previous?.total?.volume?.value ?: 0f
    val percent = if (comparable && previousVolume > 0f && volume >= 0f) {
        ((volume - previousVolume) / previousVolume * 100f)
            .takeIf { it.isFinite() && abs(it) <= 999f }
            ?.roundToInt()
    } else {
        null
    }
    val percentText = percent?.let { if (it > 0) "+$it%" else "$it%" }
    val percentDescription = percentText?.let {
        AppTokens.strings.res(Res.string.exercise_history_volume_delta, it)
    }
    val previousDate = previous?.createdAt?.value?.let {
        DateTimeUtils.format(value = it, format = DateFormat.DateOnly.DateDdMmm)
    } ?: previous?.createdAt?.display
    val caption = previous?.let {
        val previousVolumeText = if (it.total.volume.value == null) {
            "0${AppTokens.strings.res(Res.string.kg)}"
        } else {
            it.total.volume.short()
        }
        AppTokens.strings.res(
            Res.string.exercise_history_previous_short,
            previousVolumeText,
            previousDate.orEmpty(),
        )
    }
    val captionStyle = AppTokens.typography.b12Med()
    val captionMinHeight = with(LocalDensity.current) { captionStyle.lineHeight.toDp() * 2 }

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
                ExerciseHistoryShimmer(
                    captionMinHeight = captionMinHeight,
                    chartHeight = chartHeight,
                )
            } else {
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
                        if (chartComparable && chartValuesSupported) {
                            ExerciseVolumeHistoryChart(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(chartHeight),
                                value = value.history.toPersistentList().add(exercise),
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
                                        !chartComparable -> Res.string.exercise_history_different_load
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
private fun ExerciseProgressCardDifferentLoadPreview() {
    val value = progressPreviewState(historyCount = 1)
    val history = value.history
    val previous = history.last()
    val bodyIterations = previous.iterations.map { iteration ->
        iteration.copy(
            externalWeight = VolumeFormatState.Empty(),
            bodyWeight = WeightFormatState.of(80f),
            bodyMultiplier = MultiplierFormatState.of(1f),
        )
    }.toPersistentList()
    val records = persistentListOf(previous.copy(iterations = bodyIterations))
    PreviewContainer {
        ExerciseProgressCard(value = value.copy(history = records))
    }
}

@AppPreview
@Composable
private fun ExerciseProgressCardMixedHistoryPreview() {
    val value = progressPreviewState(historyCount = 2)
    val history = value.history
    val records = history.mapIndexed { index, exercise ->
        if (index == 0) {
            val iterations = exercise.iterations.map { iteration ->
                iteration.copy(
                    externalWeight = VolumeFormatState.Empty(),
                    bodyWeight = WeightFormatState.of(80f),
                    bodyMultiplier = MultiplierFormatState.of(1f),
                )
            }.toPersistentList()
            exercise.copy(iterations = iterations)
        } else {
            exercise
        }
    }.toPersistentList()
    PreviewContainer {
        ExerciseProgressCard(value = value.copy(history = records))
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
        value = LocalDateTime(year = 2026, monthNumber = 10, dayOfMonth = 16, hour = 18, minute = 0),
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy,
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
                    monthNumber = 10,
                    dayOfMonth = index * 3 + 1,
                    hour = 18,
                    minute = 0,
                ),
                range = DateRangePresets.infinity(),
                format = DateFormat.DateOnly.DateMmmDdYyyy,
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
