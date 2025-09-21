package com.grippo.design.components.example

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public enum class ExerciseExampleImageStyle {
    SMALL,
    MEDIUM,
}

@Composable
public fun ExerciseExampleImage(
    modifier: Modifier = Modifier,
    style: ExerciseExampleImageStyle,
    value: String?
) {
    when (style) {
        ExerciseExampleImageStyle.SMALL -> AsyncImage(
            modifier = modifier
                .clip(RoundedCornerShape(AppTokens.dp.exerciseExampleImage.small.radius))
                .size(AppTokens.dp.exerciseExampleImage.small.size),
            model = value,
            contentScale = ContentScale.Crop,
            contentDescription = null
        )

        ExerciseExampleImageStyle.MEDIUM -> AsyncImage(
            modifier = modifier
                .clip(RoundedCornerShape(AppTokens.dp.exerciseExampleImage.medium.radius))
                .size(AppTokens.dp.exerciseExampleImage.medium.size),
            model = value,
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleImagePreview() {
    PreviewContainer {
        ExerciseExampleImage(
            value = "https://example.com/image.jpg",
            style = ExerciseExampleImageStyle.MEDIUM
        )

        ExerciseExampleImage(
            value = "https://example.com/image.jpg",
            style = ExerciseExampleImageStyle.SMALL
        )
    }
}