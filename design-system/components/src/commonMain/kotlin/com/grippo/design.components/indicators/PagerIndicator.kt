package com.grippo.design.components.indicators

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    AnimatedContent(
        modifier = modifier,
        targetState = pageCount > 1,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 300))
        },
        label = "pager-indicator-visibility",
    ) { visible ->
        if (visible) {
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
        } else {
            Spacer(
                modifier = Modifier.height(
                    AppTokens.dp.pagerIndicator.dotSize +
                            AppTokens.dp.contentPadding.subContent * 2
                )
            )
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
