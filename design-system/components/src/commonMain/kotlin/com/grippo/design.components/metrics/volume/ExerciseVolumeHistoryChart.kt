package com.grippo.design.components.metrics.volume

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.chart.bar.BarData
import com.grippo.chart.bar.BarEntry
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.design.components.chart.internal.BarChart
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_history_current_date
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

@Composable
public fun ExerciseVolumeHistoryChart(
    modifier: Modifier = Modifier,
    value: ImmutableList<ExerciseState>,
) {
    if (value.isEmpty()) {
        return
    }

    val palette = AppTokens.colors.palette.palette7BlueGrowth
    val dates = value.map { entry ->
        entry.createdAt.value?.let { date ->
            DateTimeUtils.format(value = date, format = DateFormat.DateOnly.DateDdMmm)
        } ?: entry.createdAt.display
    }
    val selectedDate = AppTokens.strings.res(Res.string.exercise_history_current_date, dates.last())
    val data = BarData(
        items = value.mapIndexed { index, entry ->
            BarEntry(
                label = dates[index],
                value = entry.total.volume.value ?: 0f,
                color = if (index == value.lastIndex) palette[2] else palette.first(),
            )
        },
    )
    val descriptions = value.mapIndexed { index, entry ->
        val label = if (index == value.lastIndex) selectedDate else dates[index]
        val volume = VolumeFormatState.shortTotal(entry.total.volume.value ?: 0f)
        "$label: $volume"
    }
    val description = descriptions.joinToString(separator = "; ")

    BarChart(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(AppTokens.dp.metrics.volume.chart.historyAspectRatio),
        data = data,
        valueFormatter = { volume -> VolumeFormatState.shortTotal(volume) },
        description = description,
    )
}

@AppPreview
@Composable
private fun ExerciseVolumeHistoryChartPreview() {
    PreviewContainer {
        ExerciseVolumeHistoryChart(
            value = exerciseVolumeHistoryPreview(1_200f, 1_400f, 1_350f, 1_500f, 1_400f, 1_600f),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseVolumeHistoryChartTwoEntriesPreview() {
    PreviewContainer {
        ExerciseVolumeHistoryChart(value = exerciseVolumeHistoryPreview(1_600f, 1_400f))
    }
}

@AppPreview
@Composable
private fun ExerciseVolumeHistoryChartZeroPreview() {
    PreviewContainer {
        ExerciseVolumeHistoryChart(value = exerciseVolumeHistoryPreview(1_400f, 0f))
        ExerciseVolumeHistoryChart(value = exerciseVolumeHistoryPreview(0f, 0f))
    }
}

@AppPreview
@Composable
private fun ExerciseVolumeHistoryChartEmptyPreview() {
    PreviewContainer {
        ExerciseVolumeHistoryChart(value = persistentListOf())
        ExerciseVolumeHistoryChart(value = exerciseVolumeHistoryPreview(1_400f))
    }
}

private fun exerciseVolumeHistoryPreview(vararg volumes: Float): ImmutableList<ExerciseState> {
    val exercise = stubExercise()
    return volumes.mapIndexed { index, volume ->
        val date = DateTimeFormatState.of(
            value = LocalDateTime(
                year = 2026,
                month = 10,
                day = 1 + index * 3,
                hour = 10,
                minute = 0,
            ),
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy,
        )
        exercise.copy(
            id = "exercise-history-$index",
            name = "Bench press",
            iterations = persistentListOf(),
            exerciseExample = exercise.exerciseExample.copy(
                id = "bench-press",
                name = "Bench press",
                weightType = WeightTypeEnumState.FREE,
                lastUsed = date,
            ),
            createdAt = date,
            total = TrainingTotalState(
                repetitions = RepetitionsFormatState.of(30),
                intensity = IntensityFormatState.of(50f),
                volume = VolumeFormatState.of(volume),
            ),
        )
    }.toPersistentList()
}
