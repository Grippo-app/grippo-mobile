package com.grippo.exercise.example.exerciseexample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.chip.Chip
import com.grippo.design.components.chip.ChipLabel
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.ChipStype
import com.grippo.design.components.chip.ChipTrailing
import com.grippo.design.components.equipment.EquipmentsCard
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.components.muscle.MuscleLoad
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.required_equipment
import com.grippo.state.exercise.examples.stubExerciseExample
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseExampleScreen(
    state: ExerciseExampleState,
    loaders: ImmutableSet<ExerciseExampleLoader>,
    contract: ExerciseExampleContract
) = BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    val example = state.example ?: return@BaseComposeScreen

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState()),
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        ExerciseExampleImage(
            modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
            value = example.value.imageUrl,
            style = ExerciseExampleImageStyle.LARGE
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            text = example.value.name,
            style = AppTokens.typography.h1(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        FlowRow(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            Chip(
                label = ChipLabel.Empty,
                value = example.value.category.title().text(),
                size = ChipSize.Small,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.static.white,
                brush = SolidColor(example.value.category.color())
            )

            Chip(
                label = ChipLabel.Empty,
                value = example.value.forceType.title().text(),
                size = ChipSize.Small,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.static.white,
                brush = SolidColor(example.value.forceType.color())
            )

            Chip(
                label = ChipLabel.Empty,
                value = example.value.weightType.title().text(),
                size = ChipSize.Small,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.static.white,
                brush = SolidColor(example.value.weightType.color())
            )

            Chip(
                label = ChipLabel.Empty,
                value = example.value.experience.title().text(),
                size = ChipSize.Small,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.static.white,
                brush = SolidColor(example.value.experience.color())
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            text = example.value.description,
            style = AppTokens.typography.b14Reg(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        if (state.muscleLoadData.items.isNotEmpty()) {
            MuscleLoad(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                    .fillMaxWidth(),
                chartData = state.muscleLoadData,
                muscleBreakdown = state.muscleLoadMuscles,
                muscleImages = state.muscleLoadImages,
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))
        }

        Text(
            modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
            text = AppTokens.strings.res(Res.string.required_equipment),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.primary
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        EquipmentsCard(
            modifier = Modifier.fillMaxWidth(),
            value = example.equipments,
            contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding)
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
    }
}

@AppPreview
@Composable
private fun ScreenPreview1() {
    PreviewContainer {
        ExerciseExampleScreen(
            state = ExerciseExampleState(
                example = stubExerciseExample(),
            ),
            contract = ExerciseExampleContract.Empty,
            loaders = persistentSetOf()
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreview2() {
    PreviewContainer {
        ExerciseExampleScreen(
            state = ExerciseExampleState(
                example = stubExerciseExample(),
            ),
            contract = ExerciseExampleContract.Empty,
            loaders = persistentSetOf()
        )
    }
}
