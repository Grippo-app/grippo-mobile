package com.grippo.design.components.example

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.components.example.internal.ExerciseExampleCardSquare
import com.grippo.design.components.example.internal.ExerciseExampleCardWide
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.exercise.examples.stubExerciseExample

@Immutable
public sealed interface ExerciseExampleCardStyle {
    @Immutable
    public data object Square : ExerciseExampleCardStyle

    @Immutable
    public data object Wide : ExerciseExampleCardStyle
}

@Composable
public fun ExerciseExampleCard(
    modifier: Modifier = Modifier,
    value: ExerciseExampleState,
    style: ExerciseExampleCardStyle,
    onClick: () -> Unit
) {
    when (style) {
        ExerciseExampleCardStyle.Square -> ExerciseExampleCardSquare(
            modifier = modifier,
            value = value,
            onClick = onClick
        )

        ExerciseExampleCardStyle.Wide -> ExerciseExampleCardWide(
            modifier = modifier,
            value = value,
            onClick = onClick
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardSquarePreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.size(250.dp),
            value = stubExerciseExample(),
            style = ExerciseExampleCardStyle.Square,
            onClick = {}
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardWidePreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubExerciseExample(),
            style = ExerciseExampleCardStyle.Wide,
            onClick = {}
        )
    }
}