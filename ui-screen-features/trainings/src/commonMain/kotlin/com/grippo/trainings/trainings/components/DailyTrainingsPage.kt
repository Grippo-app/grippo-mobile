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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.trainings.TimelineState
import com.grippo.core.state.trainings.TimelineState.Companion.exercise
import com.grippo.core.state.trainings.stubDailyTrainingTimeline
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.datetime.TimelineIndicator
import com.grippo.design.components.empty.EmptyState
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Menu
import com.grippo.trainings.factory.timelineStyle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun DailyTrainingsPage(
    modifier: Modifier = Modifier,
    items: ImmutableList<TimelineState.Daily.Item>,
    contentPadding: PaddingValues,
    onTrainingMenuClick: (String) -> Unit,
    onExerciseClick: (String) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyState(
            modifier = modifier.fillMaxSize()
        )
        return
    }

    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding
    ) {
        items(
            items = items,
            key = { it.key },
            contentType = { it::class }
        ) { value ->
            DailyTimelineItem(
                value = value,
                onTrainingMenuClick = onTrainingMenuClick,
                onExerciseClick = onExerciseClick
            )
        }
    }
}

@Composable
private fun DailyTimelineItem(
    value: TimelineState.Daily.Item,
    onTrainingMenuClick: (String) -> Unit,
    onExerciseClick: (String) -> Unit,
) {
    val style = remember(value) { value.timelineStyle() }
    val exercise = remember(value) { value.exercise() }

    TimelineIndicator(
        modifier = Modifier.fillMaxWidth(),
        style = style
    ) {
        if (value is TimelineState.DateTime) {
            val clickProvider = remember(value.trainingId) {
                { onTrainingMenuClick(value.trainingId) }
            }

            Row(
                modifier = Modifier.padding(vertical = AppTokens.dp.contentPadding.subContent),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))

                Text(
                    text = "${value.startAt.display} - ${value.createAt.display}",
                    style = AppTokens.typography.h6(),
                    color = AppTokens.colors.text.primary
                )

                Spacer(Modifier.width(AppTokens.dp.contentPadding.content))

                Text(
                    text = value.duration.display,
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.tertiary
                )

                Spacer(Modifier.weight(1f))

                Button(
                    content = ButtonContent.Icon(
                        icon = ButtonIcon.Icon(AppTokens.icons.Menu),
                    ),
                    style = ButtonStyle.Transparent,
                    size = ButtonSize.Small,
                    onClick = clickProvider
                )
            }
        }

        if (exercise != null) {
            val clickProvider = remember(exercise.id) {
                { onExerciseClick(exercise.id) }
            }

            ExerciseCard(
                modifier = Modifier
                    .padding(vertical = AppTokens.dp.contentPadding.subContent)
                    .fillMaxWidth(),
                value = exercise,
                style = ExerciseCardStyle.Medium(clickProvider)
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
