package com.grippo.design.components.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.design.components.training.internal.ExerciseCardMedium
import com.grippo.design.components.training.internal.ExerciseCardSmall
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.stubExercise

@Immutable
public sealed interface ExerciseCardStyle {
    @Immutable
    public data class Medium(
        val onClick: () -> Unit
    ) : ExerciseCardStyle

    @Immutable
    public data class Small(
        val onClick: () -> Unit
    ) : ExerciseCardStyle
}

@Composable
public fun ExerciseCard(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    style: ExerciseCardStyle,
) {

    when (style) {
        is ExerciseCardStyle.Medium -> ExerciseCardMedium(
            modifier = modifier,
            value = value,
            onClick = style.onClick
        )

        is ExerciseCardStyle.Small -> ExerciseCardSmall(
            modifier = modifier,
            value = value,
            onClick = style.onClick
        )
    }
}

@AppPreview
@Composable
private fun ExerciseCardPreview() {
    PreviewContainer {
        ExerciseCard(
            value = stubExercise(),
            style = ExerciseCardStyle.Small(
                onClick = {}
            ),
        )
        ExerciseCard(
            value = stubExercise(),
            style = ExerciseCardStyle.Medium(
                onClick = {}
            ),
        )
    }
}
