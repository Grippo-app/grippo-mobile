package com.grippo.design.components.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Question

@Immutable
public enum class ExerciseExampleImageStyle {
    SMALL,
    MEDIUM,
    LARGE,
}

@Composable
public fun ExerciseExampleImage(
    modifier: Modifier = Modifier,
    style: ExerciseExampleImageStyle,
    value: String?
) {
    val fallback = rememberInsetVectorPainter(AppTokens.icons.Question, 12.dp)

    when (style) {
        ExerciseExampleImageStyle.MEDIUM -> AsyncImage(
            modifier = modifier
                .clip(RoundedCornerShape(AppTokens.dp.exerciseExampleImage.medium.radius))
                .size(AppTokens.dp.exerciseExampleImage.medium.size)
                .background(AppTokens.colors.background.card),
            model = value,
            contentScale = ContentScale.Crop,
            contentDescription = null,
            placeholder = fallback,
            error = fallback
        )

        ExerciseExampleImageStyle.LARGE -> AsyncImage(
            modifier = modifier
                .clip(RoundedCornerShape(AppTokens.dp.exerciseExampleImage.large.radius))
                .size(AppTokens.dp.exerciseExampleImage.large.size)
                .background(AppTokens.colors.background.card),
            model = value,
            contentScale = ContentScale.Crop,
            contentDescription = null,
            placeholder = fallback,
            error = fallback
        )

        ExerciseExampleImageStyle.SMALL -> AsyncImage(
            modifier = modifier
                .clip(RoundedCornerShape(AppTokens.dp.exerciseExampleImage.small.radius))
                .size(AppTokens.dp.exerciseExampleImage.small.size)
                .background(AppTokens.colors.background.card),
            model = value,
            contentScale = ContentScale.Crop,
            contentDescription = null,
            placeholder = fallback,
            error = fallback
        )
    }
}

@Composable
private fun rememberInsetVectorPainter(
    imageVector: ImageVector,
    padding: Dp,
): Painter {
    val base = rememberVectorPainter(image = imageVector)
    return remember(base, padding) {
        InsetPainter(base = base, padding = padding)
    }
}

private class InsetPainter(
    private val base: Painter,
    private val padding: Dp,
) : Painter() {

    private var appliedAlpha: Float = 1f
    private var appliedColorFilter: ColorFilter? = null

    override val intrinsicSize: Size
        get() = base.intrinsicSize

    override fun applyAlpha(alpha: Float): Boolean {
        appliedAlpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        appliedColorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        val p = padding.toPx()
        val w = (size.width - 2f * p).coerceAtLeast(0f)
        val h = (size.height - 2f * p).coerceAtLeast(0f)

        if (w <= 0f || h <= 0f) return

        withTransform({ translate(left = p, top = p) }) {
            with(base) {
                draw(
                    size = Size(w, h),
                    alpha = appliedAlpha,
                    colorFilter = appliedColorFilter
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun ExerciseExampleImagePreview() {
    PreviewContainer {
        ExerciseExampleImage(
            value = "https://example.com/image.jpg",
            style = ExerciseExampleImageStyle.SMALL
        )

        ExerciseExampleImage(
            value = "https://example.com/image.jpg",
            style = ExerciseExampleImageStyle.MEDIUM
        )

        ExerciseExampleImage(
            value = "https://example.com/image.jpg",
            style = ExerciseExampleImageStyle.LARGE
        )
    }
}