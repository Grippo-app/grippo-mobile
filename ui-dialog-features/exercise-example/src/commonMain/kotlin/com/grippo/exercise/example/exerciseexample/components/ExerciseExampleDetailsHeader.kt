package com.grippo.exercise.example.exerciseexample.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.design.components.example.ExampleTypeSection
import com.grippo.design.components.example.ExerciseExampleImage
import com.grippo.design.components.example.ExerciseExampleImageStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun ExerciseExampleDetailsHeader(
    modifier: Modifier,
    value: ExerciseExampleState,
) {
    Column(modifier = modifier) {
        ExerciseExampleImage(
            modifier = Modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
            value = value.value.imageUrl,
            style = ExerciseExampleImageStyle.LARGE
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            text = value.value.name,
            style = AppTokens.typography.h1(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        ExampleTypeSection(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth(),
            value = value.value
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleDetailsHeaderPreview() {
    PreviewContainer {
        ExerciseExampleDetailsHeader(
            modifier = Modifier.fillMaxWidth(),
            value = stubExerciseExample(),
        )
    }
}
