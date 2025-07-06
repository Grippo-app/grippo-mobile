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
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.equipment.EquipmentsCard
import com.grippo.design.components.example.ExerciseExampleBundlesCard
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.overview
import com.grippo.presentation.api.exercise.example.models.stubExerciseExample
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseExampleScreen(
    state: ExerciseExampleState,
    loaders: ImmutableSet<ExerciseExampleLoader>,
    contract: ExerciseExampleContract
) = BaseComposeScreen(AppTokens.colors.background.primary) {

    val example = state.example ?: return@BaseComposeScreen

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.overview),
        onClose = contract::dismiss
    )

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
            text = example.value.name,
            style = AppTokens.typography.h1(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = example.value.description,
            style = AppTokens.typography.b14Reg(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Muscles in use",
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        ExerciseExampleBundlesCard(
            modifier = Modifier.fillMaxWidth(),
            value = example.bundles
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Equipments",
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        EquipmentsCard(
            modifier = Modifier.fillMaxWidth(),
            value = example.equipments
        )
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