package com.grippo.design.components.user

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.grippo.chart.sparkline.SparklineData
import com.grippo.chart.sparkline.SparklinePoint
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.profile.WeightHistoryState
import com.grippo.core.state.profile.stubWeightHistory
import com.grippo.design.components.chart.internal.Sparkline
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil

@Composable
public fun WeightHistoryChart(
    modifier: Modifier = Modifier,
    list: ImmutableList<WeightHistoryState>,
) {
    val entries = remember(list) {
        list.filter { entry ->
            entry.value.value?.let { it.isFinite() && it > 0f } == true
        }.sortedBy { it.createdAt.value }
    }
    if (entries.isEmpty()) {
        return
    }

    val dateLabels = entries.map { it.createdAt.short() }
    val valueLabels = entries.associate { entry ->
        entry.value.value to VolumeFormatState.shortTotal(entry.value.value)
    }
    val data = remember(entries, dateLabels) {
        val origin = entries.first().createdAt.value
        val hasDates = entries.all { it.createdAt.value != null }
        val points = entries.mapIndexedNotNull { index, entry ->
            val value = entry.value.value ?: return@mapIndexedNotNull null
            val date = entry.createdAt.value
            val elapsedDays = if (hasDates && origin != null && date != null) {
                val days = origin.date.daysUntil(date.date)
                val seconds = date.time.toSecondOfDay() - origin.time.toSecondOfDay()
                (days.toDouble() + seconds / 86_400.0).toFloat()
            } else {
                index.toFloat()
            }
            SparklinePoint(x = elapsedDays, y = value, label = dateLabels[index])
        }
        SparklineData(points = points)
    }
    val description = entries.mapIndexed { index, entry ->
        "${dateLabels[index]}: ${valueLabels.getValue(entry.value.value)}"
    }.joinToString("; ")

    Sparkline(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(AppTokens.dp.weightHistoryCard.chartAspectRatio)
            .semantics { contentDescription = description },
        data = data,
        valueFormatter = valueLabels::getValue,
    )
}

@AppPreview
@Composable
private fun WeightHistoryChartPreview() {
    PreviewContainer {
        WeightHistoryChart(
            list = weightHistoryPreview(82.4f, 81.8f, 80.6f),
        )
    }
}

@AppPreview
@Composable
private fun WeightHistoryChartSingleValuePreview() {
    PreviewContainer {
        WeightHistoryChart(
            list = weightHistoryPreview(80.2f),
        )
    }
}

@AppPreview
@Composable
private fun WeightHistoryChartEmptyPreview() {
    PreviewContainer {
        WeightHistoryChart(list = persistentListOf())
    }
}

@AppPreview
@Composable
private fun WeightHistoryChartFlatPreview() {
    PreviewContainer {
        WeightHistoryChart(list = weightHistoryPreview(80.2f, 80.2f, 80.2f))
    }
}

private fun weightHistoryPreview(vararg weights: Float): ImmutableList<WeightHistoryState> {
    return weights.mapIndexed { index, weight ->
        stubWeightHistory().copy(
            id = "weight-$index",
            value = WeightFormatState.of(weight),
            createdAt = DateTimeFormatState.of(
                value = LocalDateTime(2026, 10, 1 + index * index * 3, 9, 0),
                range = DateRangePresets.infinity(),
                format = DateFormat.DateOnly.DateDdMmmYyyy,
            ),
        )
    }.toImmutableList()
}
