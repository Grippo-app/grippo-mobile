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
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.chip.Chip
import com.grippo.design.components.chip.ChipLabel
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.ChipStype
import com.grippo.design.components.chip.ChipTrailing
import com.grippo.design.components.equipment.EquipmentsCard
import com.grippo.design.components.example.ExerciseExampleBundlesCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.required_equipment
import com.grippo.design.resources.provider.select
import com.grippo.state.exercise.examples.ExerciseExampleDialogView
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
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
        ) {
            Chip(
                label = ChipLabel.Empty,
                value = example.value.category.title().text(),
                size = ChipSize.Small,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.text.primary,
                brush = SolidColor(AppTokens.colors.background.screen)
            )

            Chip(
                label = ChipLabel.Empty,
                value = example.value.forceType.title().text(),
                size = ChipSize.Small,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.text.primary,
                brush = SolidColor(AppTokens.colors.background.screen)
            )

            Chip(
                label = ChipLabel.Empty,
                value = example.value.weightType.title().text(),
                size = ChipSize.Small,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.text.primary,
                brush = SolidColor(AppTokens.colors.background.screen)
            )

            Chip(
                label = ChipLabel.Empty,
                value = example.value.experience.title().text(),
                size = ChipSize.Small,
                stype = ChipStype.Default,
                trailing = ChipTrailing.Empty,
                contentColor = AppTokens.colors.text.primary,
                brush = SolidColor(AppTokens.colors.background.screen)
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

        ExerciseExampleBundlesCard(
            modifier = Modifier.fillMaxWidth(),
            value = example.bundles
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

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

        if (state.view != ExerciseExampleDialogView.PICK) {
            Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
        }
    }

    if (state.view == ExerciseExampleDialogView.PICK) {
        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Button(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.select),
            ),
            style = ButtonStyle.Primary,
            onClick = contract::onSelectClick
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
                view = ExerciseExampleDialogView.READ
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
                view = ExerciseExampleDialogView.PICK
            ),
            contract = ExerciseExampleContract.Empty,
            loaders = persistentSetOf()
        )
    }
}