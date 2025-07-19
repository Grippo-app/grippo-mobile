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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeDialog
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.TonnageChip
import com.grippo.design.components.training.IterationsCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.icons.NavArrowRight
import com.grippo.design.resources.overview
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
            .padding(vertical = AppTokens.dp.contentPadding.content)
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            text = exercise.name,
            style = AppTokens.typography.h1(),
            color = AppTokens.colors.text.primary,
        )

        if (exercise.iterations.isNotEmpty()) {

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

            IterationsCard(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                value = exercise.iterations
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                TonnageChip(
                    modifier = Modifier,
                    value = exercise.volume
                )

                IntensityChip(
                    modifier = Modifier,
                    value = exercise.intensity
                )

                RepetitionsChip(
                    modifier = Modifier.fillMaxWidth(),
                    value = exercise.repetitions
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        val example = exercise.exerciseExample

        if (example != null) {

            val onExampleDetailsClick = remember {
                { contract.onExampleDetailsClick(example.id) }
            }

            HorizontalDivider(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                color = AppTokens.colors.divider.primary
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

            Row(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = example.name,
                    style = AppTokens.typography.b14Bold(),
                    color = AppTokens.colors.text.primary,
                )

                Button(
                    onClick = onExampleDetailsClick,
                    style = ButtonStyle.Transparent,
                    text = AppTokens.strings.res(Res.string.overview),
                    endIcon = AppTokens.icons.NavArrowRight
                )
            }

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

            Text(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
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