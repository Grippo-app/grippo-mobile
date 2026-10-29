package com.grippo.design.components.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ErrorResult
import coil3.request.NullRequestData
import coil3.request.NullRequestDataException
import coil3.request.SuccessResult
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.dialog_close
import com.grippo.design.resources.provider.exercise_image
import com.grippo.design.resources.provider.exercise_image_open
import com.grippo.design.resources.provider.exercise_image_unavailable
import com.grippo.design.resources.provider.icons.Cancel
import com.grippo.design.resources.provider.icons.Dumbbell
import com.grippo.design.resources.provider.loading
import com.grippo.design.resources.provider.onboarding_hero

@Immutable
public enum class ExerciseExampleImageStyle {
    SMALL,
    COMPACT,
    MEDIUM,
    LARGE,
}

@Composable
public fun ExerciseExampleImage(
    modifier: Modifier = Modifier,
    value: String?,
    style: ExerciseExampleImageStyle,
) {
    val dimensions = AppTokens.dp.exerciseExampleImage
    val size = when (style) {
        ExerciseExampleImageStyle.SMALL -> dimensions.small.size
        ExerciseExampleImageStyle.COMPACT -> dimensions.compact.size
        ExerciseExampleImageStyle.MEDIUM -> dimensions.medium.size
        ExerciseExampleImageStyle.LARGE -> dimensions.large.size
    }
    val radius = when (style) {
        ExerciseExampleImageStyle.SMALL -> dimensions.small.radius
        ExerciseExampleImageStyle.COMPACT -> dimensions.compact.radius
        ExerciseExampleImageStyle.MEDIUM -> dimensions.medium.radius
        ExerciseExampleImageStyle.LARGE -> dimensions.large.radius
    }
    val placeholderPadding = when (style) {
        ExerciseExampleImageStyle.SMALL -> dimensions.small.placeholderPadding
        ExerciseExampleImageStyle.COMPACT -> dimensions.compact.placeholderPadding
        ExerciseExampleImageStyle.MEDIUM -> dimensions.medium.placeholderPadding
        ExerciseExampleImageStyle.LARGE -> dimensions.large.placeholderPadding
    }
    val shape = RoundedCornerShape(radius)
    val openLabel = AppTokens.strings.res(Res.string.exercise_image_open)
    val imageLabel = AppTokens.strings.res(Res.string.exercise_image)
    val loadingLabel = AppTokens.strings.res(Res.string.loading)
    val unavailableLabel = AppTokens.strings.res(Res.string.exercise_image_unavailable)

    key(value) {
        var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
        val visibility = remember { MutableTransitionState(false) }
        val painter = (imageState as? AsyncImagePainter.State.Success)?.painter
        val isLoading = !value.isNullOrBlank() &&
            (imageState is AsyncImagePainter.State.Empty || imageState is AsyncImagePainter.State.Loading)
        val clickModifier = if (painter != null) {
            Modifier.clickable(onClickLabel = openLabel, role = Role.Button) {
                visibility.targetState = true
            }
        } else {
            Modifier
        }

        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(AppTokens.colors.background.card)
                .border(dimensions.borderWidth, AppTokens.colors.border.default, shape)
                .then(clickModifier),
            contentAlignment = Alignment.Center,
        ) {
            if (!value.isNullOrBlank()) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = value,
                    contentScale = ContentScale.Crop,
                    contentDescription = if (painter != null) imageLabel else null,
                    onState = { imageState = it },
                )
            }

            when {
                isLoading -> SkeletonBone(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            contentDescription = loadingLabel
                            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                        },
                    shape = shape,
                )

                painter == null -> Icon(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(placeholderPadding),
                    imageVector = AppTokens.icons.Dumbbell,
                    tint = AppTokens.colors.icon.secondary,
                    contentDescription = unavailableLabel,
                )
            }
        }

        if (painter != null && (visibility.currentState || visibility.targetState || !visibility.isIdle)) {
            ExerciseExampleImageOverlay(value = value, painter = painter, visibility = visibility)
        }
    }
}

@Composable
private fun ExerciseExampleImageOverlay(
    value: String?,
    painter: Painter,
    visibility: MutableTransitionState<Boolean>,
) {
    Dialog(
        onDismissRequest = { visibility.targetState = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visibleState = visibility,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180)),
        ) {
            ExerciseExampleImageViewer(
                imageModifier = Modifier.animateEnterExit(
                    enter = scaleIn(tween(220), initialScale = 0.94f),
                    exit = scaleOut(tween(180), targetScale = 0.96f),
                ),
                value = value,
                painter = painter,
                onClose = { visibility.targetState = false },
            )
        }
    }
}

@Composable
private fun ExerciseExampleImageViewer(
    modifier: Modifier = Modifier,
    value: String?,
    painter: Painter,
    imageModifier: Modifier = Modifier,
    onClose: () -> Unit,
) {
    val closeLabel = AppTokens.strings.res(Res.string.dialog_close)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTokens.colors.background.screen)
            .safeDrawingPadding()
            .padding(AppTokens.dp.contentPadding.content),
    ) {
        Button(
            modifier = Modifier
                .align(Alignment.End)
                .semantics { contentDescription = closeLabel },
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Cancel)),
            style = ButtonStyle.Tertiary,
            size = ButtonSize.Small,
            onClick = onClose,
        )
        AsyncImage(
            modifier = imageModifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
            model = value,
            placeholder = painter,
            error = painter,
            fallback = painter,
            contentDescription = AppTokens.strings.res(Res.string.exercise_image),
            contentScale = ContentScale.Fit,
        )
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
private fun rememberExerciseImagePreviewHandler(): AsyncImagePreviewHandler {
    val painter = AppTokens.drawables.res(Res.drawable.onboarding_hero)
    return remember(painter) {
        AsyncImagePreviewHandler { _, request ->
            AsyncImagePainter.State.Success(
                painter = painter,
                result = SuccessResult(image = ColorImage(0), request = request),
            )
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@AppPreview
@Composable
private fun ExerciseExampleImageLoadedSlotPreview() {
    PreviewContainer {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides rememberExerciseImagePreviewHandler()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExerciseExampleImageStyle.entries.forEach { style ->
                    ExerciseExampleImage(value = "loaded", style = style)
                }
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
            listOf("loading", null, "", "unavailable").forEach { value ->
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

@OptIn(ExperimentalCoilApi::class)
@AppPreview
@Composable
private fun ExerciseExampleImageFullscreenPreview() {
    PreviewContainer {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides rememberExerciseImagePreviewHandler()) {
            ExerciseExampleImageViewer(
                value = "loaded",
                painter = AppTokens.drawables.res(Res.drawable.onboarding_hero),
                onClose = {},
            )
        }
    }
}
