package com.grippo.design.components.example

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.design.components.example.internal.ExerciseExampleCardLarge
import com.grippo.design.components.example.internal.ExerciseExampleCardMedium
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public sealed interface ExerciseExampleCardStyle {
    @Immutable
    public data object Medium : ExerciseExampleCardStyle

    @Immutable
    public data class Large(
        val allowUsageLabel: Boolean
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
        )

        is ExerciseExampleCardStyle.Large -> ExerciseExampleCardLarge(
            modifier = modifier,
            value = value,
            allowUsageLabel = style.allowUsageLabel
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardLargePreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.size(250.dp),
            value = stubExerciseExample(),
            style = ExerciseExampleCardStyle.Large(allowUsageLabel = true),
        )

        ExerciseExampleCard(
            modifier = Modifier.size(250.dp),
            value = stubExerciseExample(),
            style = ExerciseExampleCardStyle.Large(allowUsageLabel = false),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleCardMediumPreview() {
    PreviewContainer {
        ExerciseExampleCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubExerciseExample(),
            style = ExerciseExampleCardStyle.Medium,
        )
    }
}