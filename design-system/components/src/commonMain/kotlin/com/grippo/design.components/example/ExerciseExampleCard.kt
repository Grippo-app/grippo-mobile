package com.grippo.design.components.example

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.design.components.example.internal.ExerciseExampleCardLarge
import com.grippo.design.components.example.internal.ExerciseExampleCardMedium
import com.grippo.design.components.example.internal.ExerciseExampleCardSmall
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public sealed interface ExerciseExampleCardStyle {
    @Immutable
    public data class Small(
        val value: ExerciseExampleValueState,
    ) : ExerciseExampleCardStyle

    @Immutable
    public data class Medium(
        val value: ExerciseExampleState,
    ) : ExerciseExampleCardStyle

    @Immutable
    public data class Large(
        val value: ExerciseExampleState,
    ) : ExerciseExampleCardStyle
}

@Composable
public fun ExerciseExampleCard(
    modifier: Modifier = Modifier,
    style: ExerciseExampleCardStyle,
) {
    when (style) {
        is ExerciseExampleCardStyle.Small -> ExerciseExampleCardSmall(
            modifier = modifier,
            value = style.value,
        )

        is ExerciseExampleCardStyle.Medium -> ExerciseExampleCardMedium(
            modifier = modifier,
            value = style.value,
        )

        is ExerciseExampleCardStyle.Large -> ExerciseExampleCardLarge(
            modifier = modifier,
            value = style.value,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardLargePreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.size(250.dp),
            style = ExerciseExampleCardStyle.Large(stubExerciseExample()),
        )

        ExerciseExampleCard(
            modifier = Modifier.size(250.dp),
            style = ExerciseExampleCardStyle.Large(stubExerciseExample()),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardMediumPreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Medium(stubExerciseExample()),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardSmallPreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            style = ExerciseExampleCardStyle.Small(stubExerciseExample().value),
        )
    }
}