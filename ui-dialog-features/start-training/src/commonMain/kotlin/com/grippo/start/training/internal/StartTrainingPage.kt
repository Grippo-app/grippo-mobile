package com.grippo.start.training.internal

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercises
import com.grippo.design.components.banner.BannerCard
import com.grippo.design.components.banner.BannerCardStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.ArrowLeft
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.icons.EmptyExercise
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
) {
    when (option) {
        StartTrainingOption.Empty -> EmptyPage(
            modifier = modifier,
            previousOption = previousOption,
            nextOption = nextOption,
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
            icon = AppTokens.icons.EmptyExercise,
            title = AppTokens.strings.res(Res.string.start_training_option_empty_title),
            description = AppTokens.strings.res(Res.string.start_training_option_empty_description),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (previousLabel != null) {
                SwipeHint(
                    modifier = Modifier.align(Alignment.BottomStart),
                    direction = SwipeHintDirection.Previous,
                    label = previousLabel,
                )
            }
            if (nextLabel != null) {
                SwipeHint(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    direction = SwipeHintDirection.Next,
                    label = nextLabel,
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
) {
    val accent = AppTokens.colors.semantic.notice

    val transition = rememberInfiniteTransition(
        label = "swipe-hint"
    )

    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val icon: ImageVector = when (direction) {
        SwipeHintDirection.Previous -> AppTokens.icons.ArrowLeft
        SwipeHintDirection.Next -> AppTokens.icons.ArrowRight
    }

    val translationSign = if (direction == SwipeHintDirection.Previous) -1f else 1f

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (direction == SwipeHintDirection.Previous) {
            AnimatedHintIcon(
                icon = icon,
                tint = accent,
                translationSign = translationSign,
                pulse = pulse,
            )
            Text(
                text = label,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.tertiary,
            )
        } else {
            Text(
                text = label,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.tertiary,
            )
            AnimatedHintIcon(
                icon = icon,
                tint = accent,
                translationSign = translationSign,
                pulse = pulse,
            )
        }
    }
}

@Composable
private fun AnimatedHintIcon(
    icon: ImageVector,
    tint: Color,
    translationSign: Float,
    pulse: Float,
) {
    val scale = SWIPE_HINT_SCALE_MIN + pulse * (SWIPE_HINT_SCALE_MAX - SWIPE_HINT_SCALE_MIN)
    Icon(
        modifier = Modifier
            .size(AppTokens.dp.training.startTraining.icon)
            .graphicsLayer {
                translationX = translationSign * pulse * SWIPE_HINT_TRAVEL.toPx()
                scaleX = scale
                scaleY = scale
            },
        imageVector = icon,
        tint = tint,
        contentDescription = null,
    )
}

private val SWIPE_HINT_TRAVEL = 4.dp
private const val SWIPE_HINT_SCALE_MIN = 0.92f
private const val SWIPE_HINT_SCALE_MAX = 1.12f

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
