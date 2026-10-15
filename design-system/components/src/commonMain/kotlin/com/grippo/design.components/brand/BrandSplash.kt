package com.grippo.design.components.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.grippo.design.components.loading.Loader
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.app_name
import com.grippo.design.resources.provider.icons.GrippoLogo

private const val ENTRANCE_DURATION_MS = 420
private const val GLOW_DURATION_MS = 2400
private const val ENTRANCE_SCALE = 0.94f
private const val GLOW_MIN_ALPHA = 0.65f

@Composable
public fun BrandSplash(
    modifier: Modifier = Modifier,
    showLoader: Boolean,
) {
    val dp = AppTokens.dp.brandSplash
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = tween(ENTRANCE_DURATION_MS, easing = FastOutSlowInEasing),
        )
    }
    val transition = rememberInfiniteTransition(label = "Brand glow")
    val glow by transition.animateFloat(
        initialValue = GLOW_MIN_ALPHA,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(GLOW_DURATION_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Glow opacity",
    )
    val glowColors = listOf(
        AppTokens.colors.illustration.backgroundStart,
        AppTokens.colors.illustration.backgroundEnd,
        Color.Transparent,
    )
    Box(
        modifier = modifier.background(AppTokens.colors.background.screen),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(dp.glowSize)
                .graphicsLayer { alpha = glow }
                .drawWithCache {
                    val brush = Brush.radialGradient(
                        colors = glowColors,
                        radius = size.minDimension / 2f,
                    )
                    onDrawBehind {
                        drawCircle(brush = brush)
                    }
                },
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block),
        ) {
            Image(
                modifier = Modifier
                    .size(dp.logoSize)
                    .graphicsLayer {
                        val scale = ENTRANCE_SCALE + (1f - ENTRANCE_SCALE) * entrance.value
                        scaleX = scale
                        scaleY = scale
                    },
                imageVector = AppTokens.icons.GrippoLogo,
                contentDescription = null,
            )
            Text(
                text = AppTokens.strings.res(Res.string.app_name),
                style = AppTokens.typography.h1(),
                color = AppTokens.colors.text.primary,
            )
        }
        if (showLoader) {
            Loader(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(AppTokens.dp.screen.verticalPadding),
            )
        }
    }
}

@AppPreview
@Composable
private fun BrandSplashPreview() {
    PreviewContainer {
        BrandSplash(modifier = Modifier.fillMaxSize(), showLoader = true)
    }
}

@AppPreview
@Composable
private fun BrandSplashIdlePreview() {
    PreviewContainer {
        BrandSplash(modifier = Modifier.fillMaxSize(), showLoader = false)
    }
}
