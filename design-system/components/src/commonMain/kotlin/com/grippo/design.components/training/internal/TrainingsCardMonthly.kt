package com.grippo.design.components.training.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.exercises
import com.grippo.design.resources.provider.sets
import com.grippo.design.resources.provider.trainings
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

@Composable
internal fun TrainingsCardMonthly(
    modifier: Modifier = Modifier,
    trainings: ImmutableList<TrainingState>,
) {
    if (trainings.isEmpty()) return

    val weekDayName = remember(trainings) {
        val date = trainings.minBy { it.createdAt }.createdAt.date
        DateTimeUtils.format(date, DateFormat.WEEKDAY_LONG)
    }

    val dateLabel = remember(trainings) {
        val start = trainings.minBy { it.createdAt }.createdAt.date
        DateTimeUtils.format(start, DateFormat.DATE_DD_MMMM)
    }

    val trainingsCount = remember(trainings) { trainings.size }

    val exercisesCount = remember(trainings) {
        trainings.sumOf { it.exercises.size }
    }

    val setsCount = remember(trainings) {
        trainings.sumOf { training ->
            training.exercises.sumOf { it.iterations.size }
        }
    }

    val totalDuration = remember(trainings) {
        trainings.fold(ZERO) { acc, training -> acc + training.duration }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppTokens.dp.trainingCard.monthly.radius))
            .background(AppTokens.colors.background.card)
            .padding(AppTokens.dp.contentPadding.block),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = weekDayName,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary
        )

        Text(
            text = dateLabel,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))

        MonthlyStatGrid(
            topRow = listOf(
                MonthlyStat(
                    label = AppTokens.strings.res(Res.string.trainings),
                    value = trainingsCount.toString()
                ),
                MonthlyStat(
                    label = AppTokens.strings.res(Res.string.exercises),
                    value = exercisesCount.toString()
                )
            ),
            bottomRow = listOf(
                MonthlyStat(
                    label = AppTokens.strings.res(Res.string.sets),
                    value = setsCount.toString()
                ),
                MonthlyStat(
                    label = AppTokens.strings.res(Res.string.duration),
                    value = formatDuration(totalDuration)
                )
            )
        )
    }
}

@Immutable
private data class MonthlyStat(
    val label: String,
    val value: String,
)

@Composable
private fun MonthlyStatGrid(
    topRow: List<MonthlyStat>,
    bottomRow: List<MonthlyStat>,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        MonthlyStatRow(values = topRow)

        MonthlyStatRow(values = bottomRow)
    }
}

@Composable
private fun MonthlyStatRow(values: List<MonthlyStat>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        values.forEach { stat ->
            MonthlyStatCell(
                modifier = Modifier.weight(1f),
                stat = stat
            )
        }
    }
}

@Composable
private fun MonthlyStatCell(
    modifier: Modifier = Modifier,
    stat: MonthlyStat,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stat.label,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.text))

        Text(
            text = stat.value,
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes
    val hours = totalMinutes / 60
    val minutes = (totalMinutes % 60).toInt()

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

@AppPreview
@Composable
private fun TrainingsCardMonthlyPreview() {
    PreviewContainer {
        TrainingsCardMonthly(
            trainings = persistentListOf(stubTraining(), stubTraining(), stubTraining()),
        )
    }
}
