package com.grippo.training.exercise

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.IntensityChipStyle
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.RepetitionsChipStyle
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.training.IterationCard
import com.grippo.design.components.training.IterationCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.add_set_btn
import com.grippo.design.resources.provider.save_btn
import com.grippo.design.resources.provider.sets_value
import com.grippo.state.trainings.stubExercise
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseScreen(
    state: ExerciseState,
    loaders: ImmutableSet<ExerciseLoader>,
    contract: ExerciseContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = state.exercise.name,
        onBack = contract::onBack,
        content = {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .padding(bottom = AppTokens.dp.contentPadding.content)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                VolumeChip(
                    value = state.exercise.metrics.volume,
                    style = VolumeChipStyle.LONG
                )

                RepetitionsChip(
                    value = state.exercise.metrics.repetitions,
                    style = RepetitionsChipStyle.LONG
                )

                IntensityChip(
                    value = state.exercise.metrics.intensity,
                    style = IntensityChipStyle.LONG
                )
            }
        }
    )

    Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

    LazyColumn(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(
            top = AppTokens.dp.contentPadding.content
        ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        item {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(
                    Res.string.sets_value,
                    state.exercise.iterations.size
                ),
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
                textAlign = TextAlign.Start
            )
        }

        itemsIndexed(
            items = state.exercise.iterations,
            key = { index, item -> item.id }
        ) { index, iteration ->
            val editVolumeProvider = remember(iteration.id) {
                { contract.onEditVolume(iteration.id) }
            }

            val editRepetitionProvider = remember(iteration.id) {
                { contract.onEditRepetition(iteration.id) }
            }

            IterationCard(
                modifier = Modifier.fillMaxWidth(),
                value = iteration,
                style = IterationCardStyle.Editable(
                    label = (index + 1).toString(),
                    onVolumeClick = editVolumeProvider,
                    onRepetitionClick = editRepetitionProvider
                )
            )
        }
    }

    Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTokens.dp.screen.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        Button(
            modifier = Modifier.weight(1f),
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.add_set_btn),
            ),
            style = ButtonStyle.Secondary,
            onClick = contract::onAddIteration
        )

        val buttonState = remember(loaders, state.exercise.iterations) {
            when {
                state.exercise.iterations.isEmpty() -> ButtonState.Disabled
                else -> ButtonState.Enabled
            }
        }

        Button(
            modifier = Modifier.weight(1f),
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.save_btn),
            ),
            style = ButtonStyle.Primary,
            state = buttonState,
            onClick = contract::onSave
        )
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ExerciseScreenPreview() {
    PreviewContainer {
        ExerciseScreen(
            state = ExerciseState(
                exercise = stubExercise()
            ),
            loaders = persistentSetOf(),
            contract = ExerciseContract.Empty
        )
    }
}
