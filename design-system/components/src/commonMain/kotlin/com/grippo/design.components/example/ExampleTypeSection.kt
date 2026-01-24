package com.grippo.design.components.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.design.components.chip.CategoryChip
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.ForceTypeChip
import com.grippo.design.components.chip.WeightTypeChip
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun ExampleTypeSection(
    modifier: Modifier = Modifier,
    value: ExerciseExampleValueState,
    size: ChipSize = ChipSize.Medium,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        CategoryChip(
            value = value.category,
            size = size
        )

        ForceTypeChip(
            value = value.forceType,
            size = size
        )

        WeightTypeChip(
            value = value.weightType,
            size = size
        )
    }
}

@AppPreview
@Composable
private fun ExampleTypeSectionPreview() {
    PreviewContainer {
        ExampleTypeSection(
            value = stubExerciseExample().value,
            size = ChipSize.Small
        )

        ExampleTypeSection(
            value = stubExerciseExample().value,
            size = ChipSize.Medium
        )
    }
}
