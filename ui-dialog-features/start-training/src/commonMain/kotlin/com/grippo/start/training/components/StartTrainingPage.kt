package com.grippo.start.training.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.grippo.core.state.trainings.stubExercises
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.empty.IllustratedEmptyState
import com.grippo.design.components.illustration.IllustrationStyle
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.empty_exercises
import com.grippo.design.resources.provider.start_training_option_empty_description
import com.grippo.design.resources.provider.start_training_option_empty_title
import com.grippo.design.resources.provider.start_training_option_preset_description
import com.grippo.design.resources.provider.start_training_option_preset_title
import com.grippo.design.resources.provider.start_training_option_recent_title
import com.grippo.design.resources.provider.start_training_recent_description
import com.grippo.start.training.StartTrainingOption
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

@Composable
internal fun StartTrainingPage(
    modifier: Modifier = Modifier,
    option: StartTrainingOption,
    contentPadding: PaddingValues,
    recentPicker: (@Composable () -> Unit)? = null,
) {
    val title = when (option) {
        StartTrainingOption.Empty -> Res.string.start_training_option_empty_title
        is StartTrainingOption.Preset -> Res.string.start_training_option_preset_title
        is StartTrainingOption.Recent -> Res.string.start_training_option_recent_title
    }
    val description = when (option) {
        StartTrainingOption.Empty -> Res.string.start_training_option_empty_description
        is StartTrainingOption.Preset -> Res.string.start_training_option_preset_description
        is StartTrainingOption.Recent -> Res.string.start_training_recent_description
    }
    val exercises = when (option) {
        StartTrainingOption.Empty -> {
            IllustratedEmptyState(
                modifier = modifier,
                painter = AppTokens.drawables.res(Res.drawable.empty_exercises),
                title = AppTokens.strings.res(title),
                text = AppTokens.strings.res(description),
                contentPadding = contentPadding,
                illustrationStyle = IllustrationStyle.Large,
            )
            return
        }
        is StartTrainingOption.Preset -> option.exercises
        is StartTrainingOption.Recent -> option.exercises
    }
    val listState = remember(option.key) { LazyListState() }
    val layoutDirection = LocalLayoutDirection.current
    val horizontalPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(layoutDirection),
        end = contentPadding.calculateEndPadding(layoutDirection),
    )

    Column(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontalPadding)
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = AppTokens.dp.contentPadding.content,
                ),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            Text(
                text = AppTokens.strings.res(title),
                style = AppTokens.typography.h3(),
                color = AppTokens.colors.text.primary,
            )
            Text(
                text = AppTokens.strings.res(description),
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
        if (recentPicker != null) {
            recentPicker()
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))
            ContentDivider()
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = listState,
            contentPadding = PaddingValues(
                top = if (recentPicker != null) AppTokens.dp.contentPadding.content else 0.dp,
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.Top,
        ) {
            itemsIndexed(
                items = exercises,
                key = { _, exercise -> exercise.id },
                contentType = { _, _ -> "exercise" },
            ) { index, exercise ->
                ExerciseCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontalPadding),
                    value = exercise,
                    style = ExerciseCardStyle.Planned,
                )
                if (index < exercises.lastIndex) {
                    ContentDivider(
                        modifier = Modifier
                            .padding(horizontalPadding)
                            .padding(
                                start = AppTokens.dp.exerciseExampleImage.compact.size +
                                        AppTokens.dp.contentPadding.subContent,
                            ),
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun StartTrainingPageEmptyPreview() {
    PreviewContainer {
        StartTrainingPage(
            modifier = Modifier.fillMaxSize(),
            option = StartTrainingOption.Empty,
            contentPadding = PaddingValues(AppTokens.dp.dialog.horizontalPadding),
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingPagePresetPreview() {
    PreviewContainer {
        StartTrainingPage(
            modifier = Modifier.fillMaxSize(),
            option = StartTrainingOption.Preset(exercises = stubExercises().take(2).toPersistentList()),
            contentPadding = PaddingValues(AppTokens.dp.dialog.horizontalPadding),
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingPageRecentPreview() {
    PreviewContainer {
        val exercises = stubExercises().take(2).toPersistentList()
        val recents = persistentListOf(
            StartTrainingOption.Recent(
                trainingId = "evening",
                createdAt = LocalDateTime(year = 2026, month = 10, day = 16, hour = 18, minute = 30),
                exercises = exercises,
            ),
            StartTrainingOption.Recent(
                trainingId = "morning",
                createdAt = LocalDateTime(year = 2026, month = 10, day = 16, hour = 8, minute = 15),
                exercises = exercises,
            ),
        )
        val selected = recents.first()

        StartTrainingPage(
            modifier = Modifier.fillMaxSize(),
            option = selected,
            contentPadding = PaddingValues(AppTokens.dp.dialog.horizontalPadding),
            recentPicker = {
                StartTrainingRecentPicker(
                    modifier = Modifier.fillMaxWidth(),
                    options = recents,
                    selectedKey = selected.key,
                    onSelect = {},
                )
            },
        )
    }
}
