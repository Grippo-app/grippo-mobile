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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.IntensityChipStyle
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.RepetitionsChipStyle
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
import com.grippo.design.components.training.IterationsCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.NavArrowRight
import com.grippo.design.resources.provider.overview
import com.grippo.state.trainings.stubExercise
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseScreen(
    state: ExerciseState,
    loaders: ImmutableSet<ExerciseLoader>,
    contract: ExerciseContract
) = BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    val exercise = state.exercise ?: return@BaseComposeScreen

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            text = exercise.name,
            style = AppTokens.typography.h1(),
            color = AppTokens.colors.text.primary,
        )

        if (exercise.iterations.isNotEmpty()) {

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

            IterationsCard(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                    .fillMaxWidth(),
                value = exercise.iterations
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                VolumeChip(
                    value = exercise.metrics.volume,
                    style = VolumeChipStyle.LONG
                )

                IntensityChip(
                    value = exercise.metrics.intensity,
                    style = IntensityChipStyle.LONG
                )

                RepetitionsChip(
                    value = exercise.metrics.repetitions,
                    style = RepetitionsChipStyle.LONG
                )
            }
        }

        val example = exercise.exerciseExample

        val onExampleDetailsClick = remember {
            { contract.onExampleDetailsClick(example.id) }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Row(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
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
                size = ButtonSize.Small,
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.overview),
                    endIcon = AppTokens.icons.NavArrowRight
                ),
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            text = example.description,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            style = AppTokens.typography.b14Reg(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
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