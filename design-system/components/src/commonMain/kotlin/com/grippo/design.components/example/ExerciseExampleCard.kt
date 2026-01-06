package com.grippo.design.components.example

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.design.components.example.internal.ExerciseExampleCardMedium
import com.grippo.design.components.example.internal.ExerciseExampleCardSmall
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public sealed interface ExerciseExampleCardStyle {
    @Immutable
    public data object Small : ExerciseExampleCardStyle

    @Immutable
    public data class Medium(
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
        is ExerciseExampleCardStyle.Small -> ExerciseExampleCardSmall(
            modifier = modifier,
            value = value,
        )

        is ExerciseExampleCardStyle.Medium -> ExerciseExampleCardMedium(
            modifier = modifier,
            value = value,
            allowUsageLabel = style.allowUsageLabel
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
            style = ExerciseExampleCardStyle.Medium(allowUsageLabel = true),
        )

        ExerciseExampleCard(
            modifier = Modifier.size(250.dp),
            value = stubExerciseExample(),
            style = ExerciseExampleCardStyle.Medium(allowUsageLabel = false),
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
            style = ExerciseExampleCardStyle.Small,
        )
    }
}