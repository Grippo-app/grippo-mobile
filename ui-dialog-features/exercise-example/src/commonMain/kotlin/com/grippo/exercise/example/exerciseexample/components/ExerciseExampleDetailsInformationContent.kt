package com.grippo.exercise.example.exerciseexample.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.design.components.equipment.EquipmentsCard
import com.grippo.design.components.example.ExampleDescriptionText
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoading
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingMode
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ExerciseExampleDetailsInformationContent(
    modifier: Modifier,
    example: ExerciseExampleState,
    muscleLoad: MuscleLoadSummaryState?,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block),
    ) {
        ExampleDescriptionText(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            text = example.value.description,
        )

        if (example.equipments.isNotEmpty()) {
            EquipmentsCard(
                modifier = Modifier.fillMaxWidth(),
                value = example.equipments,
                contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding)
            )
        }

        muscleLoad
            ?.takeIf { it.perGroup.entries.isNotEmpty() }
            ?.let { summary ->
                MuscleLoading(
                    modifier = Modifier
                        .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                        .fillMaxWidth(),
                    summary = summary,
                    mode = MuscleLoadingMode.PerGroup,
                )
            }
    }
}

@AppPreview
@Composable
private fun ExerciseExampleDetailsInformationContentPreview() {
    PreviewContainer {
        ExerciseExampleDetailsInformationContent(
            modifier = Modifier.fillMaxWidth(),
            example = stubExerciseExample(),
            muscleLoad = stubMuscleLoadSummary(),
        )
    }
}

@AppPreview
@Composable
private fun EquipmentWithoutDescriptionPreview() {
    val example = stubExerciseExample()
    PreviewContainer {
        ExerciseExampleDetailsInformationContent(
            modifier = Modifier.fillMaxWidth(),
            example = example.copy(
                value = example.value.copy(id = "equipment-only", description = ""),
                equipments = persistentListOf(EquipmentState(id = "barbell", type = EquipmentEnumState.BARBELL)),
            ),
            muscleLoad = null,
        )
    }
}

@AppPreview
@Composable
private fun AnatomyWithoutDescriptionPreview() {
    val example = stubExerciseExample()
    val summary = stubMuscleLoadSummary()
    PreviewContainer {
        ExerciseExampleDetailsInformationContent(
            modifier = Modifier.fillMaxWidth(),
            example = example.copy(
                value = example.value.copy(id = "anatomy-only", description = ""),
                equipments = persistentListOf(),
            ),
            muscleLoad = summary.copy(meta = summary.meta.copy(trainingsCount = 0, totalSets = 0)),
        )
    }
}
