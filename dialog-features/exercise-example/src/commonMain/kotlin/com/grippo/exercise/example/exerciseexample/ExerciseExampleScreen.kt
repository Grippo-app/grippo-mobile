package com.grippo.exercise.example.exerciseexample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.equipment.EquipmentsCard
import com.grippo.design.components.example.ExerciseExampleBundlesCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.required_equipment
import com.grippo.state.exercise.examples.stubExerciseExample
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseExampleScreen(
    state: ExerciseExampleState,
    loaders: ImmutableSet<ExerciseExampleLoader>,
    contract: ExerciseExampleContract
) = BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.secondary)) {

    val example = state.example ?: return@BaseComposeScreen

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            text = example.value.name,
            style = AppTokens.typography.h1(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            text = example.value.description,
            style = AppTokens.typography.b14Reg(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

        ExerciseExampleBundlesCard(
            modifier = Modifier.fillMaxWidth(),
            value = example.bundles
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

        HorizontalDivider(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            color = AppTokens.colors.divider.secondary
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Text(
            modifier = Modifier.padding(horizontal = AppTokens.dp.screen.horizontalPadding),
            text = AppTokens.strings.res(Res.string.required_equipment),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.primary
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        EquipmentsCard(
            modifier = Modifier.fillMaxWidth(),
            value = example.equipments,
            contentPadding = PaddingValues(horizontal = AppTokens.dp.screen.horizontalPadding)
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
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