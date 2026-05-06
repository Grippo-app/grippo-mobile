package com.grippo.start.training.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercises
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.start_training_exercises_count
import com.grippo.design.resources.provider.start_training_option_empty_description
import com.grippo.design.resources.provider.start_training_option_empty_title
import com.grippo.design.resources.provider.start_training_option_preset_description
import com.grippo.design.resources.provider.start_training_option_preset_title
import com.grippo.design.resources.provider.start_training_option_recent_title
import com.grippo.start.training.StartTrainingOption
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun StartTrainingPage(
    modifier: Modifier = Modifier,
    option: StartTrainingOption,
) {
    when (option) {
        StartTrainingOption.Empty -> EmptyPage(
            modifier = modifier
        )

        is StartTrainingOption.Preset -> ExercisesPage(
            modifier = modifier,
            title = AppTokens.strings.res(Res.string.start_training_option_preset_title),
            subtitle = AppTokens.strings.res(Res.string.start_training_option_preset_description),
            exercises = option.exercises,
        )

        is StartTrainingOption.Recent -> ExercisesPage(
            modifier = modifier,
            title = AppTokens.strings.res(Res.string.start_training_option_recent_title),
            subtitle = option.createdAt.display.takeIf { it.isNotBlank() },
            exercises = option.exercises,
        )
    }
}

@Composable
private fun EmptyPage(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.exerciseCard.small.radius)
            )
            .padding(AppTokens.dp.contentPadding.block),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
        ) {
            Text(
                text = AppTokens.strings.res(Res.string.start_training_option_empty_title),
                style = AppTokens.typography.h3(),
                color = AppTokens.colors.text.primary,
                textAlign = TextAlign.Center,
            )

            Text(
                text = AppTokens.strings.res(Res.string.start_training_option_empty_description),
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ExercisesPage(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    exercises: ImmutableList<ExerciseState>,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center,
        )

        val countLabel = AppTokens.strings.res(
            Res.string.start_training_exercises_count,
            exercises.size.toString()
        )

        val combinedSubtitle = listOfNotNull(subtitle, countLabel)
            .filter { it.isNotBlank() }
            .joinToString(separator = SUBTITLE_SEPARATOR)

        if (combinedSubtitle.isNotBlank()) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = combinedSubtitle,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            contentPadding = PaddingValues(bottom = AppTokens.dp.contentPadding.subContent),
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
}

private const val SUBTITLE_SEPARATOR = " · "

@AppPreview
@Composable
private fun StartTrainingPageEmptyPreview() {
    PreviewContainer {
        StartTrainingPage(
            option = StartTrainingOption.Empty
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingPagePresetPreview() {
    PreviewContainer {
        StartTrainingPage(
            option = StartTrainingOption.Preset(stubExercises())
        )
    }
}

@AppPreview
@Composable
private fun StartTrainingPageRecentPreview() {
    PreviewContainer {
        StartTrainingPage(
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
