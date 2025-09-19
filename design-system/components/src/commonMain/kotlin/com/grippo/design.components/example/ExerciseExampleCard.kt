package com.grippo.design.components.example

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.components.example.internal.ExerciseExampleCardLarge
import com.grippo.design.components.example.internal.ExerciseExampleCardMedium
import com.grippo.design.components.example.internal.ExerciseExampleCardSmall
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.exercise.examples.stubExerciseExample

@Immutable
public sealed interface ExerciseExampleCardStyle {
    @Immutable
    public data class Small(
        val onCardClick: () -> Unit,
    ) : ExerciseExampleCardStyle

    @Immutable
    public data class Medium(
        val onCardClick: () -> Unit,
        val onDetailsClick: () -> Unit
    ) : ExerciseExampleCardStyle

    @Immutable
    public data class Large(
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
        is ExerciseExampleCardStyle.Medium -> ExerciseExampleCardMedium(
            modifier = modifier,
            value = value,
            onCardClick = style.onCardClick,
            onDetailsClick = style.onDetailsClick
        )

        is ExerciseExampleCardStyle.Large -> ExerciseExampleCardLarge(
            modifier = modifier,
            value = value,
            onCardClick = style.onCardClick
        )

        is ExerciseExampleCardStyle.Small -> ExerciseExampleCardSmall(
            modifier = modifier,
            value = value,
            onCardClick = style.onCardClick
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardSMediumPreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.size(250.dp),
            value = stubExerciseExample(),
            style = ExerciseExampleCardStyle.Medium({}, {}),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardLargePreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubExerciseExample(),
            style = ExerciseExampleCardStyle.Large({}),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardSmallPreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubExerciseExample(),
            style = ExerciseExampleCardStyle.Small({}),
        )
    }
}