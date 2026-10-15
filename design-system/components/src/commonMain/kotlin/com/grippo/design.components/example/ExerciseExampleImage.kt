package com.grippo.design.components.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ErrorResult
import coil3.request.NullRequestData
import coil3.request.NullRequestDataException
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.QuestionCircle

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
    val dimensions = AppTokens.dp.exerciseExampleImage
    val size = when (style) {
        ExerciseExampleImageStyle.SMALL -> dimensions.small.size
        ExerciseExampleImageStyle.MEDIUM -> dimensions.medium.size
        ExerciseExampleImageStyle.LARGE -> dimensions.large.size
    }
    val radius = when (style) {
        ExerciseExampleImageStyle.SMALL -> dimensions.small.radius
        ExerciseExampleImageStyle.MEDIUM -> dimensions.medium.radius
        ExerciseExampleImageStyle.LARGE -> dimensions.large.radius
    }
    val placeholderPadding = when (style) {
        ExerciseExampleImageStyle.SMALL -> dimensions.small.placeholderPadding
        ExerciseExampleImageStyle.MEDIUM -> dimensions.medium.placeholderPadding
        ExerciseExampleImageStyle.LARGE -> dimensions.large.placeholderPadding
    }
    val shape = RoundedCornerShape(radius)
    val fallback = rememberInsetVectorPainter(
        imageVector = AppTokens.icons.QuestionCircle,
        padding = placeholderPadding,
        tint = AppTokens.colors.icon.secondary,
    )

    AsyncImage(
        modifier = modifier
            .clip(shape)
            .size(size)
            .background(AppTokens.colors.background.card)
            .border(dimensions.borderWidth, AppTokens.colors.border.default, shape),
        model = value,
        contentScale = ContentScale.Crop,
        contentDescription = null,
        placeholder = fallback,
        error = fallback
    )
}

@Composable
private fun rememberInsetVectorPainter(
    imageVector: ImageVector,
    padding: Dp,
    tint: Color,
): Painter {
    val base = rememberVectorPainter(image = imageVector)
    return remember(base, padding, tint) {
        InsetPainter(base = base, padding = padding, tint = ColorFilter.tint(tint))
    }
}

private class InsetPainter(
    private val base: Painter,
    private val padding: Dp,
    private val tint: ColorFilter,
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
                    colorFilter = appliedColorFilter ?: tint
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun ExerciseExampleImageLoadedSlotPreview() {
    PreviewContainer {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExerciseExampleImageStyle.entries.forEach { style ->
                ExerciseExampleImage(value = "https://example.com/image.jpg", style = style)
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@AppPreview
@Composable
private fun ExerciseExampleImageFallbackStatesPreview() {
    PreviewContainer {
        val handler = AsyncImagePreviewHandler { _, request ->
            when (request.data) {
                "loading" -> AsyncImagePainter.State.Loading(null)
                else -> AsyncImagePainter.State.Error(
                    painter = null,
                    result = ErrorResult(
                        image = null,
                        request = request,
                        throwable = if (request.data == NullRequestData) {
                            NullRequestDataException()
                        } else {
                            IllegalStateException("Preview image unavailable")
                        },
                    ),
                )
            }
        }
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides handler) {
            listOf("loading", null, "unavailable").forEach { value ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExerciseExampleImageStyle.entries.forEach { style ->
                        ExerciseExampleImage(value = value, style = style)
                    }
                }
            }
        }
    }
}
