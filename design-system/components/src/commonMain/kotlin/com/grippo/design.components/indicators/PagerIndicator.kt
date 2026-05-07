package com.grippo.design.components.indicators

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

private const val ANIMATION_DURATION_MS = 220

@Composable
public fun PagerIndicator(
    modifier: Modifier = Modifier,
    pageCount: Int,
    selectedIndex: Int,
) {
    val visibleState = remember {
        MutableTransitionState(initialState = false)
    }.apply { targetState = pageCount > 1 }

    AnimatedVisibility(
        modifier = modifier,
        visibleState = visibleState,
        enter = fadeIn(animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)) +
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(durationMillis = ANIMATION_DURATION_MS),
                ),
        exit = fadeOut(animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)) +
                scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(durationMillis = ANIMATION_DURATION_MS),
                ),
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = AppTokens.colors.semantic.notice.copy(0.18f),
                    shape = CircleShape,
                ).padding(
                    horizontal = AppTokens.dp.contentPadding.content,
                    vertical = AppTokens.dp.contentPadding.subContent,
                ),
            horizontalArrangement = Arrangement.spacedBy(
                AppTokens.dp.pagerIndicator.dotSpacing,
                Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pageCount) { index ->
                val selected = index == selectedIndex

                val width by animateDpAsState(
                    targetValue = if (selected) {
                        AppTokens.dp.pagerIndicator.dotSizeActive
                    } else {
                        AppTokens.dp.pagerIndicator.dotSize
                    },
                    animationSpec = tween(durationMillis = ANIMATION_DURATION_MS),
                    label = "indicatorWidth"
                )

                val color by animateColorAsState(
                    targetValue = if (selected) {
                        AppTokens.colors.semantic.notice
                    } else {
                        AppTokens.colors.text.primary
                    },
                    animationSpec = tween(durationMillis = ANIMATION_DURATION_MS),
                    label = "indicatorColor"
                )

                Box(
                    modifier = Modifier
                        .height(AppTokens.dp.pagerIndicator.dotSize)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun PagerIndicatorPreview() {
    PreviewContainer {
        PagerIndicator(pageCount = 1, selectedIndex = 0)
        PagerIndicator(pageCount = 4, selectedIndex = 0)
        PagerIndicator(pageCount = 4, selectedIndex = 2)
    }
}
