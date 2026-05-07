package com.grippo.start.training.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercises
import com.grippo.design.components.banner.BannerCard
import com.grippo.design.components.banner.BannerCardStyle
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.empty.EmptyState
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.empty_training_exercises
import com.grippo.design.resources.provider.icons.ArrowLeft
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.icons.EmptyExercise
import com.grippo.design.resources.provider.icons.Muscle
import com.grippo.design.resources.provider.icons.Sparkle
import com.grippo.design.resources.provider.icons.Timer
import com.grippo.design.resources.provider.start_training_exercises_count
import com.grippo.design.resources.provider.start_training_option_empty_description
import com.grippo.design.resources.provider.start_training_option_empty_title
import com.grippo.design.resources.provider.start_training_option_preset_description
import com.grippo.design.resources.provider.start_training_option_preset_title
import com.grippo.design.resources.provider.start_training_option_recent_title
import com.grippo.design.resources.provider.today
import com.grippo.design.resources.provider.yesterday
import com.grippo.start.training.StartTrainingOption
import com.grippo.start.training.overlayReservedHeight
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun StartTrainingPage(
    modifier: Modifier = Modifier,
    option: StartTrainingOption,
    previousOption: StartTrainingOption? = null,
    nextOption: StartTrainingOption? = null,
    onSwipePrevious: () -> Unit = {},
    onSwipeNext: () -> Unit = {},
) {
    when (option) {
        StartTrainingOption.Empty -> EmptyPage(
            modifier = modifier,
            previousOption = previousOption,
            nextOption = nextOption,
            onSwipePrevious = onSwipePrevious,
            onSwipeNext = onSwipeNext,
        )

        is StartTrainingOption.Preset -> ExercisesPage(
            modifier = modifier,
            header = ExerciseHeader.Preset,
            exercises = option.exercises,
        )

        is StartTrainingOption.Recent -> ExercisesPage(
            modifier = modifier,
            header = ExerciseHeader.Recent(option.createdAt),
            exercises = option.exercises,
        )
    }
}

@Immutable
private sealed interface ExerciseHeader {
    @Immutable
    data object Preset : ExerciseHeader

    @Immutable
    data class Recent(val createdAt: DateTimeFormatState) : ExerciseHeader
}

@Composable
private fun EmptyPage(
    modifier: Modifier = Modifier,
    previousOption: StartTrainingOption? = null,
    nextOption: StartTrainingOption? = null,
    onSwipePrevious: () -> Unit = {},
    onSwipeNext: () -> Unit = {},
) {
    val previousLabel = previousOption?.hint()
    val nextLabel = nextOption?.hint()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = overlayReservedHeight),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        BannerCard(
            modifier = Modifier.fillMaxWidth(),
            style = BannerCardStyle.Custom(AppTokens.colors.text.tertiary),
            icon = AppTokens.icons.Muscle,
            title = AppTokens.strings.res(Res.string.start_training_option_empty_title),
            description = AppTokens.strings.res(Res.string.start_training_option_empty_description),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            EmptyState(
                modifier = Modifier.fillMaxSize(),
                value = AppTokens.icons.EmptyExercise,
                text = AppTokens.strings.res(Res.string.empty_training_exercises),
            )

            if (previousLabel != null) {
                SwipeHint(
                    modifier = Modifier.align(Alignment.BottomStart),
                    direction = SwipeHintDirection.Previous,
                    label = previousLabel,
                    onClick = onSwipePrevious,
                )
            }
            if (nextLabel != null) {
                SwipeHint(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    direction = SwipeHintDirection.Next,
                    label = nextLabel,
                    onClick = onSwipeNext,
                )
            }
        }
    }
}

@Immutable
private enum class SwipeHintDirection {
    Previous,
    Next
}

@Composable
private fun SwipeHint(
    modifier: Modifier = Modifier,
    direction: SwipeHintDirection,
    label: String,
    onClick: () -> Unit,
) {
    val arrow = ButtonIcon.Icon(
        when (direction) {
            SwipeHintDirection.Previous -> AppTokens.icons.ArrowLeft
            SwipeHintDirection.Next -> AppTokens.icons.ArrowRight
        }
    )

    Button(
        modifier = modifier,
        content = ButtonContent.Text(
            text = label,
            startIcon = arrow.takeIf { direction == SwipeHintDirection.Previous },
            endIcon = arrow.takeIf { direction == SwipeHintDirection.Next },
        ),
        style = ButtonStyle.Transparent,
        size = ButtonSize.Small,
        onClick = onClick,
    )
}

@Composable
private fun ExercisesPage(
    modifier: Modifier = Modifier,
    header: ExerciseHeader,
    exercises: ImmutableList<ExerciseState>,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        ExerciseHeaderBanner(
            modifier = Modifier.fillMaxWidth(),
            header = header,
            exercisesCount = exercises.size,
        )

        BottomOverlayContainer(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            overlay = AppTokens.colors.background.dialog,
            contentPadding = PaddingValues(vertical = AppTokens.dp.contentPadding.subContent),
            bottom = {
                Spacer(modifier = Modifier.height(overlayReservedHeight))
            },
            content = { containerModifier, resolvedPadding ->
                LazyColumn(
                    modifier = containerModifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                    contentPadding = resolvedPadding,
                ) {
                    items(exercises, key = { it.id }) { exercise ->
                        ExerciseCard(
                            modifier = Modifier.fillMaxWidth(),
                            value = exercise,
                            style = ExerciseCardStyle.Medium {},
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun ExerciseHeaderBanner(
    modifier: Modifier = Modifier,
    header: ExerciseHeader,
    exercisesCount: Int,
) {
    val style: BannerCardStyle
    val icon: ImageVector
    val title: String
    val description: String

    when (header) {
        ExerciseHeader.Preset -> {
            style = BannerCardStyle.Notice
            icon = AppTokens.icons.Sparkle
            title = AppTokens.strings.res(Res.string.start_training_option_preset_title)
            description = AppTokens.strings.res(Res.string.start_training_option_preset_description)
        }

        is ExerciseHeader.Recent -> {
            style = BannerCardStyle.Info
            icon = AppTokens.icons.Timer
            title = AppTokens.strings.res(Res.string.start_training_option_recent_title)
            description = rememberRecentDescription(header.createdAt)
        }
    }

    BannerCard(
        modifier = modifier,
        style = style,
        icon = icon,
        title = title,
        description = description,
        trailing = AppTokens.strings.res(
            Res.string.start_training_exercises_count,
            exercisesCount.toString(),
        ),
    )
}

@Composable
private fun rememberRecentDescription(state: DateTimeFormatState): String {
    val today = AppTokens.strings.res(Res.string.today)
    val yesterday = AppTokens.strings.res(Res.string.yesterday)
    val value = state.value

    return remember(value, today, yesterday, state.display) {
        if (value == null) return@remember state.display

        val time = DateTimeUtils.format(value.time, DateFormat.TimeOnly.Time24hHm)
        when {
            DateTimeUtils.isToday(value.date) -> "$today, $time"
            DateTimeUtils.isYesterday(value.date) -> "$yesterday, $time"
            else -> {
                val date = DateTimeUtils.format(value, DateFormat.DateOnly.DateMmmDdComma)
                "$date, $time"
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
            option = StartTrainingOption.Empty
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingPageEmptyWithBothHintsPreview() {
    PreviewContainer {
        StartTrainingPage(
            modifier = Modifier.fillMaxSize(),
            option = StartTrainingOption.Empty,
            previousOption = StartTrainingOption.Preset(stubExercises()),
            nextOption = StartTrainingOption.Recent(
                trainingId = "stub",
                createdAt = DateTimeFormatState.of(
                    value = DateTimeUtils.now(),
                    range = DateRangePresets.infinity(),
                    format = DateFormat.DateOnly.DateMmmDdYyyy,
                ),
                exercises = stubExercises(),
            ),
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingPageEmptyWithPresetHintPreview() {
    PreviewContainer {
        StartTrainingPage(
            modifier = Modifier.fillMaxSize(),
            option = StartTrainingOption.Empty,
            previousOption = StartTrainingOption.Preset(stubExercises()),
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingPageEmptyWithRecentHintPreview() {
    PreviewContainer {
        StartTrainingPage(
            modifier = Modifier.fillMaxSize(),
            option = StartTrainingOption.Empty,
            nextOption = StartTrainingOption.Recent(
                trainingId = "stub",
                createdAt = DateTimeFormatState.of(
                    value = DateTimeUtils.now(),
                    range = DateRangePresets.infinity(),
                    format = DateFormat.DateOnly.DateMmmDdYyyy,
                ),
                exercises = stubExercises(),
            ),
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingPagePresetPreview() {
    PreviewContainer {
        StartTrainingPage(
            modifier = Modifier.fillMaxSize(),
            option = StartTrainingOption.Preset(stubExercises())
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingPageRecentPreview() {
    PreviewContainer {
        StartTrainingPage(
            modifier = Modifier.fillMaxSize(),
            option = StartTrainingOption.Recent(
                trainingId = "stub",
                createdAt = DateTimeFormatState.of(
                    value = DateTimeUtils.now(),
                    range = DateRangePresets.infinity(),
                    format = DateFormat.DateOnly.DateMmmDdYyyy,
                ),
                exercises = stubExercises(),
            )
        )
    }
}
