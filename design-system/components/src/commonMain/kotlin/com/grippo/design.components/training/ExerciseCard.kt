package com.grippo.design.components.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.design.components.training.internal.ExerciseCardLarge
import com.grippo.design.components.training.internal.ExerciseCardMedium
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public sealed interface ExerciseCardStyle {
    @Immutable
    public data class Large(
        val onClick: () -> Unit
    ) : ExerciseCardStyle

    @Immutable
    public data class Medium(
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
        is ExerciseCardStyle.Large -> ExerciseCardLarge(
            modifier = modifier,
            value = value,
            onClick = style.onClick
        )

        is ExerciseCardStyle.Medium -> ExerciseCardMedium(
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
            style = ExerciseCardStyle.Medium(
                onClick = {}
            ),
        )
        ExerciseCard(
            value = stubExercise(),
            style = ExerciseCardStyle.Large(
                onClick = {}
            ),
        )
    }
}
