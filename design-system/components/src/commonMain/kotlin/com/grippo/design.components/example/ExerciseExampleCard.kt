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
    public data class Square(
        val onCardClick: () -> Unit,
        val onDetailsClick: () -> Unit
    ) : ExerciseExampleCardStyle

    @Immutable
    public data class Wide(
        val onCardClick: () -> Unit,
    ) : ExerciseExampleCardStyle
}

@Composable
public fun ExerciseExampleCard(
    modifier: Modifier = Modifier,
    value: ExerciseExampleState,
    style: ExerciseExampleCardStyle,
) {
    when (style) {
        is ExerciseExampleCardStyle.Square -> ExerciseExampleCardSquare(
            modifier = modifier,
            value = value,
            onCardClick = style.onCardClick,
            onDetailsClick = style.onDetailsClick
        )

        is ExerciseExampleCardStyle.Wide -> ExerciseExampleCardWide(
            modifier = modifier,
            value = value,
            onCardClick = style.onCardClick
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
            style = ExerciseExampleCardStyle.Square({}, {}),
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
            style = ExerciseExampleCardStyle.Wide({}),
        )
    }
}