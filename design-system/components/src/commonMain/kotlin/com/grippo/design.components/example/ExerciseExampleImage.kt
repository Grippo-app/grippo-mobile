package com.grippo.design.components.example

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.coroutines.launch

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
    val positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()

    TooltipBox(
        positionProvider = positionProvider,
        state = tooltipState,
        tooltip = {
            AsyncImage(
                modifier = Modifier
                    .padding(
                        vertical = AppTokens.dp.exerciseExampleImage.tooltip.verticalPadding,
                        horizontal = AppTokens.dp.exerciseExampleImage.tooltip.horizontalPadding
                    )
                    .clip(RoundedCornerShape(AppTokens.dp.exerciseExampleImage.tooltip.radius))
                    .size(AppTokens.dp.exerciseExampleImage.tooltip.size),
                model = value,
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        },
        content = {
            when (style) {
                ExerciseExampleImageStyle.SMALL -> AsyncImage(
                    modifier = modifier
                        .scalableClick(onClick = { scope.launch { tooltipState.show() } })
                        .clip(RoundedCornerShape(AppTokens.dp.exerciseExampleImage.small.radius))
                        .size(AppTokens.dp.exerciseExampleImage.small.size),
                    model = value,
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )

                ExerciseExampleImageStyle.MEDIUM -> AsyncImage(
                    modifier = modifier
                        .scalableClick(onClick = { scope.launch { tooltipState.show() } })
                        .clip(RoundedCornerShape(AppTokens.dp.exerciseExampleImage.medium.radius))
                        .size(AppTokens.dp.exerciseExampleImage.medium.size),
                    model = value,
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )
            }
        }
    )
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