package com.grippo.exercise.example.exerciseexample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeDialog
import com.grippo.core.ScreenBackground
import com.grippo.design.components.equipment.EquipmentsCard
import com.grippo.design.components.example.ExerciseExampleBundlesCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.presentation.api.exercise.example.models.stubExerciseExample
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseExampleScreen(
    state: ExerciseExampleState,
    loaders: ImmutableSet<ExerciseExampleLoader>,
    contract: ExerciseExampleContract
) = BaseComposeDialog(background = ScreenBackground.Color(AppTokens.colors.background.secondary)) {

    val example = state.example ?: return@BaseComposeDialog

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

        EquipmentsCard(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            value = example.equipments
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