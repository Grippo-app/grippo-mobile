package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.modifiers.illustrationBackground
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.home_last_workout_in_period
import com.grippo.design.resources.provider.metric_overview_duration
import com.grippo.design.resources.provider.metric_overview_volume
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
public fun LastTrainingCard(
    modifier: Modifier = Modifier,
    value: TrainingState,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(AppTokens.dp.metrics.panel.large.radius)
    val createdAt = value.createdAt
    val createdAtValue = createdAt.value
    val createdAtText = if (createdAtValue != null) {
        DateCompose.rememberFormat(value = createdAtValue, format = createdAt.format)
    } else {
        createdAt.display
    }

    Column(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .clip(shape)
            .illustrationBackground(shape)
            .padding(
                horizontal = AppTokens.dp.metrics.panel.large.horizontalPadding,
                vertical = AppTokens.dp.metrics.panel.large.verticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Text(
                text = AppTokens.strings.res(Res.string.home_last_workout_in_period),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
            Text(
                text = createdAtText,
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            RecapMetric(
                modifier = Modifier.weight(1f),
                value = value.duration.short(),
                label = AppTokens.strings.res(Res.string.metric_overview_duration),
            )
            RecapMetric(
                modifier = Modifier.weight(1f),
                value = value.total.volume.short(),
                label = AppTokens.strings.res(Res.string.metric_overview_volume),
            )
        }
    }
}

@Composable
private fun RecapMetric(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = value,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
        )
        Text(
            text = label,
            style = AppTokens.typography.b11Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@AppPreview
@Composable
private fun LastTrainingCardPreview() {
    PreviewContainer {
        val training = lastTrainingPreviewValue()
        LastTrainingCard(
            value = training,
            onClick = {}
        )
    }
}

@AppPreview
@Composable
private fun LastTrainingCardLongValuesPreview() {
    PreviewContainer {
        val training = lastTrainingPreviewValue()
        LastTrainingCard(
            value = training.copy(
                duration = DurationFormatState.of(23.hours + 59.minutes),
                total = training.total.copy(volume = VolumeFormatState.of(184_829.5f)),
            ),
            onClick = {}
        )
    }
}

@AppPreview
@Composable
private fun LastTrainingCardSingleExercisePreview() {
    PreviewContainer {
        val training = lastTrainingPreviewValue()
        LastTrainingCard(
            value = training.copy(exercises = training.exercises.take(1).toPersistentList()),
            onClick = {}
        )
    }
}

private fun lastTrainingPreviewValue(): TrainingState {
    val training = stubTraining()
    return training.copy(
        id = "last-training-preview",
        duration = DurationFormatState.of(75.minutes),
        createdAt = DateTimeFormatState.of(
            value = LocalDateTime(2026, 10, 10, 9, 0),
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateDdMmmYyyy,
        ),
        total = training.total.copy(volume = VolumeFormatState.of(3_240f)),
        exercises = training.exercises.mapIndexed { index, exercise ->
            exercise.copy(id = "last-training-exercise-$index")
        }.toPersistentList(),
    )
}
