package com.grippo.exercise

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeDialog
import com.grippo.core.ScreenBackground
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.TonnageChip
import com.grippo.design.components.training.IterationsCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.exercise_details
import com.grippo.presentation.api.trainings.models.stubExercise
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseScreen(
    state: ExerciseState,
    loaders: ImmutableSet<ExerciseLoader>,
    contract: ExerciseContract
) = BaseComposeDialog(background = ScreenBackground.Color(AppTokens.colors.background.secondary)) {

    val exercise = state.exercise ?: return@BaseComposeDialog

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = AppTokens.dp.screen.horizontalPadding,
                vertical = AppTokens.dp.contentPadding.content
            )
    ) {

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = exercise.name,
            style = AppTokens.typography.h1(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Volume: ${exercise.volume}",
            style = AppTokens.typography.b14Reg(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Repetitions: ${exercise.repetitions}",
            style = AppTokens.typography.b14Reg(),
            color = AppTokens.colors.text.primary,
        )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Intensity: ${exercise.intensity}",
            style = AppTokens.typography.b14Reg(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

        val example = exercise.exerciseExample

        if (example != null) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = AppTokens.colors.divider.primary
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.exercise_details),
                style = AppTokens.typography.b14Bold(),
                color = AppTokens.colors.text.primary,
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = example.description,
                style = AppTokens.typography.b14Reg(),
                color = AppTokens.colors.text.primary,
            )
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ExerciseScreen(
            state = ExerciseState(
                exercise = stubExercise(),
            ),
            contract = ExerciseContract.Empty,
            loaders = persistentSetOf()
        )
    }
}