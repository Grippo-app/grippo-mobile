package com.grippo.design.components.metrics.performance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.grippo.chart.sparkline.SparklineData
import com.grippo.chart.sparkline.SparklinePoint
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.performance.EstimatedOneRepMaxState
import com.grippo.core.state.metrics.performance.stubEstimatedOneRepMax
import com.grippo.design.components.chart.internal.Sparkline
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.estimated_one_rm
import com.grippo.design.resources.provider.estimated_one_rm_history_peak
import com.grippo.design.resources.provider.estimated_one_rm_source
import com.grippo.design.resources.provider.estimated_one_rm_unavailable
import com.grippo.design.resources.provider.listed_pair
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.daysUntil

@Composable
public fun EstimatedOneRepMaxCard(
    modifier: Modifier = Modifier,
    state: EstimatedOneRepMaxState?,
) {
    val entries = state?.entries

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.estimated_one_rm),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )

        if (entries.isNullOrEmpty()) {
            Text(
                text = AppTokens.strings.res(Res.string.estimated_one_rm_unavailable),
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
            )
            return@Column
        }

        val latest = entries.last()
        val peak = entries.maxOf { it.value }
        val latestLabel = VolumeFormatState.shortTotal(latest.value)
        val sourceLabel = VolumeFormatState.shortTotal(latest.weight)
        val peakLabel = VolumeFormatState.shortTotal(peak)
        val valueLabels = entries.associate { entry ->
            entry.value to VolumeFormatState.shortTotal(entry.value)
        }
        val date = DateCompose.rememberFormat(value = latest.start, format = DateFormat.DateOnly.DateDdMmmYyyy)
        val time = DateCompose.rememberFormat(value = latest.start, format = DateFormat.TimeOnly.Time24hHm)
        val origin = entries.first().start
        val points = entries.map { entry ->
            val pointDate = DateCompose.rememberFormat(
                value = entry.start,
                format = DateFormat.DateOnly.DateDdMmmYyyy,
            )
            val pointTime = DateCompose.rememberFormat(value = entry.start, format = DateFormat.TimeOnly.Time24hHm)
            val days = origin.date.daysUntil(entry.start.date)
            val seconds = entry.start.time.toSecondOfDay() - origin.time.toSecondOfDay()
            val elapsedDays = days.toDouble() + seconds / 86_400.0
            SparklinePoint(
                x = elapsedDays.toFloat(),
                y = entry.value,
                label = AppTokens.strings.res(Res.string.listed_pair, pointDate, pointTime),
            )
        }
        val graphDescription = entries.zip(points).joinToString("; ") { (entry, point) ->
            "${point.label}: ${valueLabels.getValue(entry.value)}"
        }

        Text(
            text = latestLabel,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
        )

        Text(
            text = AppTokens.strings.res(
                Res.string.estimated_one_rm_source,
                sourceLabel,
                latest.repetitions.toString(),
                AppTokens.strings.res(Res.string.listed_pair, date, time),
            ),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )

        Text(
            text = AppTokens.strings.res(
                Res.string.estimated_one_rm_history_peak,
                peakLabel,
            ),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.tertiary,
        )

        Sparkline(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTokens.dp.metrics.performance.strength.height)
                .semantics { contentDescription = graphDescription },
            data = SparklineData(points = points),
            valueFormatter = valueLabels::getValue,
        )
    }
}

@AppPreview
@Composable
private fun EstimatedOneRepMaxCardPreview() {
    PreviewContainer {
        EstimatedOneRepMaxCard(state = stubEstimatedOneRepMax())
    }
}

@AppPreview
@Composable
private fun EstimatedOneRepMaxCardSinglePointPreview() {
    val state = stubEstimatedOneRepMax()
    PreviewContainer {
        EstimatedOneRepMaxCard(state = state.copy(entries = persistentListOf(state.entries.last())))
    }
}

@AppPreview
@Composable
private fun EstimatedOneRepMaxCardEmptyPreview() {
    PreviewContainer {
        EstimatedOneRepMaxCard(state = EstimatedOneRepMaxState(entries = persistentListOf()))
    }
}
