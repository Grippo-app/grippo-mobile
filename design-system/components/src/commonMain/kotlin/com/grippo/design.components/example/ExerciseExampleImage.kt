package com.grippo.design.components.example

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.grippo.design.core.AppTokens

@Composable
public fun ExerciseExampleImage(
    modifier: Modifier = Modifier,
    value: String?
) {
    AsyncImage(
        modifier = modifier
            .clip(RoundedCornerShape(AppTokens.dp.exerciseExampleImage.radius))
            .size(AppTokens.dp.exerciseExampleImage.size),
        model = value,
        contentScale = ContentScale.Crop,
        contentDescription = null
    )
}
