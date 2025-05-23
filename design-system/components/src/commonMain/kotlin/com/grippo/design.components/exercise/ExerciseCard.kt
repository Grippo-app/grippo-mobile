package com.grippo.design.components.exercise

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.presentation.api.trainings.models.ExerciseState
import com.grippo.presentation.api.trainings.models.stubExercise

@Composable
public fun ExerciseCard(
    modifier: Modifier = Modifier,
    value: ExerciseState
) {
    Text(
        text = value.name,

        )
}

@AppPreview
@Composable
private fun ExerciseCardPreview() {
    PreviewContainer {
        ExerciseCard(
            value = stubExercise()
        )
    }
}