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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.equipment.EquipmentsCard
import com.grippo.design.components.muscle.MuscleBundleCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.exercise_example_equipments
import com.grippo.design.resources.exercise_example_muscles
import com.grippo.presentation.api.exercise.example.models.stubExerciseExample
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExerciseExampleScreen(
    state: ExerciseExampleState,
    loaders: ImmutableSet<ExerciseExampleLoader>,
    contract: ExerciseExampleContract
) = BaseComposeScreen(AppTokens.colors.background.secondary) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = AppTokens.dp.screen.horizontalPadding,
                vertical = AppTokens.dp.screen.verticalPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val example = state.example ?: return@Column

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = example.value.name,
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = example.value.description,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(24.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.exercise_example_equipments),
            style = AppTokens.typography.h3(),
            textAlign = TextAlign.Center,
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(12.dp))

        EquipmentsCard(
            modifier = Modifier.fillMaxWidth(),
            value = example.equipments
        )

        Spacer(modifier = Modifier.size(16.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.exercise_example_muscles),
            style = AppTokens.typography.h3(),
            textAlign = TextAlign.Center,
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(12.dp))

        MuscleBundleCard(
            modifier = Modifier.fillMaxWidth(),
            value = example.bundles
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