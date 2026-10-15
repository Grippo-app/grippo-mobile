package com.grippo.design.components.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.examples.stubExerciseExampleValueState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.design.components.chip.CategoryChip
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.ForceTypeChip
import com.grippo.design.components.chip.WeightTypeChip
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateFormat

@Composable
public fun ExampleTypeSection(
    modifier: Modifier = Modifier,
    value: ExerciseExampleValueState,
    size: ChipSize = ChipSize.Medium,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        itemVerticalAlignment = Alignment.CenterVertically,
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
private fun ExampleTypeSectionSmallPreview() {
    PreviewContainer {
        ExampleTypeSectionVariants(size = ChipSize.Small)
    }
}

@AppPreview
@Composable
private fun ExampleTypeSectionMediumPreview() {
    PreviewContainer {
        ExampleTypeSectionVariants(size = ChipSize.Medium)
    }
}

@AppPreview
@Composable
private fun ExampleTypeSectionContextsPreview() {
    PreviewContainer {
        val example = stubExerciseExample().let {
            it.copy(
                value = it.value.copy(
                    id = "example-type-context-preview",
                    category = CategoryEnumState.ISOLATION,
                    forceType = ForceTypeEnumState.HINGE,
                    weightType = WeightTypeEnumState.FIXED,
                    lastUsed = DateTimeFormatState.Empty(format = DateFormat.DateOnly.DateDdMmm),
                ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTokens.colors.background.dialog)
                .padding(AppTokens.dp.dialog.horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            Text(
                text = example.value.name,
                style = AppTokens.typography.h1(),
                color = AppTokens.colors.text.primary,
            )
            ExampleTypeSection(value = example.value)
        }

        ExerciseExampleCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
            style = ExerciseExampleCardStyle.Large(example),
        )

        val shape = RoundedCornerShape(AppTokens.dp.iterationCard.radius)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .background(AppTokens.colors.background.card, shape)
                .border(AppTokens.dp.iterationCard.borderWidth, AppTokens.colors.border.default, shape)
                .padding(AppTokens.dp.contentPadding.content),
        ) {
            ExerciseExampleCard(style = ExerciseExampleCardStyle.Medium(example))
        }
    }
}

@Composable
private fun ExampleTypeSectionVariants(size: ChipSize) {
    val example = stubExerciseExampleValueState().copy(
        id = "example-type-preview",
        lastUsed = DateTimeFormatState.Empty(format = DateFormat.DateOnly.DateDdMmm),
    )

    ExampleTypeSection(value = example, size = size)
    ExampleTypeSection(
        value = example.copy(
            category = CategoryEnumState.ISOLATION,
            forceType = ForceTypeEnumState.PULL,
            weightType = WeightTypeEnumState.FREE,
        ),
        size = size,
    )
    ExampleTypeSection(
        value = example.copy(
            forceType = ForceTypeEnumState.HINGE,
            weightType = WeightTypeEnumState.FIXED,
        ),
        size = size,
    )
}
