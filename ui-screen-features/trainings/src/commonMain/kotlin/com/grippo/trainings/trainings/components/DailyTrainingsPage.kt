package com.grippo.trainings.trainings.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.trainings.TimelineState
import com.grippo.core.state.trainings.TimelineState.Companion.exercise
import com.grippo.core.state.trainings.stubDailyTrainingTimeline
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.datetime.TimelineIndicator
import com.grippo.design.components.empty.IllustratedEmptyState
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.digest_unavailable
import com.grippo.design.resources.provider.digest_zero_duration
import com.grippo.design.resources.provider.empty_daily_trainings
import com.grippo.design.resources.provider.empty_workouts
import com.grippo.design.resources.provider.icons.MoreVertical
import com.grippo.design.resources.provider.menu
import com.grippo.trainings.factory.timelineStyle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.time.Duration

@Composable
internal fun DailyTrainingsPage(
    modifier: Modifier = Modifier,
    items: ImmutableList<TimelineState.Daily.Item>,
    contentPadding: PaddingValues,
    onTrainingMenuClick: (String) -> Unit,
    onExerciseClick: (String) -> Unit,
) {
    if (items.isEmpty()) {
        IllustratedEmptyState(
            modifier = modifier.fillMaxSize(),
            painter = AppTokens.drawables.res(Res.drawable.empty_workouts),
            text = AppTokens.strings.res(Res.string.empty_daily_trainings),
            contentPadding = contentPadding,
        )
        return
    }

    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
    ) {
        items(
            items = items,
            key = { it.key },
            contentType = { it::class },
        ) { value ->
            DailyTimelineItem(
                value = value,
                onTrainingMenuClick = onTrainingMenuClick,
                onExerciseClick = onExerciseClick,
            )
        }
    }
}

@Composable
private fun DailyTimelineItem(
    modifier: Modifier = Modifier,
    value: TimelineState.Daily.Item,
    onTrainingMenuClick: (String) -> Unit,
    onExerciseClick: (String) -> Unit,
) {
    val style = value.timelineStyle()
    val exercise = value.exercise()

    TimelineIndicator(
        modifier = modifier.fillMaxWidth(),
        style = style,
    ) {
        if (value is TimelineState.DateTime) {
            val menuLabel = AppTokens.strings.res(Res.string.menu)
            val start = (value.startAt as? DateTimeFormatState.Valid)?.display
                ?: AppTokens.strings.res(Res.string.digest_unavailable)
            val duration = when (value.duration.value) {
                null -> AppTokens.strings.res(Res.string.digest_unavailable)
                Duration.ZERO -> AppTokens.strings.res(Res.string.digest_zero_duration)
                else -> value.duration.short()
            }

            Row(
                modifier = Modifier.padding(vertical = AppTokens.dp.contentPadding.subContent),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(AppTokens.dp.contentPadding.subContent))

                Text(
                    text = "$start – ${value.createAt.display}",
                    style = AppTokens.typography.b14Semi(),
                    color = AppTokens.colors.text.primary,
                )

                Spacer(modifier = Modifier.width(AppTokens.dp.contentPadding.content))

                Text(
                    text = duration,
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    modifier = Modifier.semantics { contentDescription = menuLabel },
                    content = ButtonContent.Icon(
                        icon = ButtonIcon.Icon(AppTokens.icons.MoreVertical),
                    ),
                    style = ButtonStyle.Tertiary,
                    size = ButtonSize.Small,
                    onClick = { onTrainingMenuClick(value.trainingId) },
                )
            }
        }

        if (exercise != null) {
            ExerciseCard(
                modifier = Modifier
                    .fillMaxWidth(),
                value = exercise,
                style = ExerciseCardStyle.Medium(onClick = { onExerciseClick(exercise.id) }),
            )
        }
    }
}

@AppPreview
@Composable
private fun DailyTrainingsPagePreview() {
    PreviewContainer {
        DailyTrainingsPage(
            items = stubDailyTrainingTimeline()
                .filterIsInstance<TimelineState.Daily.Item>()
                .toPersistentList(),
            contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
            onTrainingMenuClick = {},
            onExerciseClick = {},
        )
    }
}

@AppPreview
@Composable
private fun DailyTrainingsEmptyPagePreview() {
    PreviewContainer {
        DailyTrainingsPage(
            items = persistentListOf(),
            contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
            onTrainingMenuClick = {},
            onExerciseClick = {},
        )
    }
}

@AppPreview
@Composable
private fun DailyTrainingsUnavailableHeaderPreview() {
    val items = stubDailyTrainingTimeline()
        .filterIsInstance<TimelineState.Daily.Item>()
        .map { item ->
            if (item is TimelineState.DateTime) {
                item.copy(
                    startAt = DateTimeFormatState.Empty(format = item.startAt.format),
                    duration = DurationFormatState.Empty(),
                )
            } else {
                item
            }
        }
        .toPersistentList()

    PreviewContainer {
        DailyTrainingsPage(
            items = items,
            contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
            onTrainingMenuClick = {},
            onExerciseClick = {},
        )
    }
}
