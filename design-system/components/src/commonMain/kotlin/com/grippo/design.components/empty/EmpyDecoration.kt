package com.grippo.design.components.empty

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.barbell
import com.grippo.design.resources.provider.box
import com.grippo.design.resources.provider.dumbbell
import com.grippo.design.resources.provider.plate
import com.grippo.design.resources.provider.weight
import kotlinx.collections.immutable.persistentListOf

@Immutable
private data class EmptyIcon(
    val painter: Painter,
    val alignment: Alignment,
    val offset: DpOffset,
    val rotation: Float,
    val size: Dp,
)

@Composable
public fun EmptyDecorations(modifier: Modifier = Modifier) {
    val largeIconSize = AppTokens.dp.empty.image
    val dumbbell = AppTokens.drawables.res(Res.drawable.dumbbell)
    val barbell = AppTokens.drawables.res(Res.drawable.barbell)
    val plate = AppTokens.drawables.res(Res.drawable.plate)
    val box = AppTokens.drawables.res(Res.drawable.box)
    val weight = AppTokens.drawables.res(Res.drawable.weight)

    val decorations = remember {
        persistentListOf(
            EmptyIcon(
                painter = dumbbell,
                alignment = Alignment.TopStart,
                offset = DpOffset(-(largeIconSize / 3), -(34.dp)),
                rotation = -12f,
                size = largeIconSize,
            ),
            EmptyIcon(
                painter = barbell,
                alignment = Alignment.TopEnd,
                offset = DpOffset((largeIconSize / 3), (92).dp),
                rotation = 0f,
                size = largeIconSize * 1.3f,
            ),
            EmptyIcon(
                painter = plate,
                alignment = Alignment.CenterStart,
                offset = DpOffset(-(largeIconSize / 2), -(34.dp)),
                rotation = -18f,
                size = largeIconSize,
            ),
            EmptyIcon(
                painter = box,
                alignment = Alignment.BottomEnd,
                offset = DpOffset((largeIconSize / 3), -(52).dp),
                rotation = -8f,
                size = largeIconSize,
            ),
            EmptyIcon(
                painter = weight,
                alignment = Alignment.BottomStart,
                offset = DpOffset(-(largeIconSize / 2), 60.dp),
                rotation = 10f,
                size = largeIconSize,
            ),
        )
    }

    Box(modifier = modifier) {
        decorations.forEach { icon ->
            Image(
                modifier = Modifier
                    .align(icon.alignment)
                    .offset(x = icon.offset.x, y = icon.offset.y)
                    .size(icon.size)
                    .scale(1.4f)
                    .alpha(0.8f)
                    .graphicsLayer {
                        rotationZ = icon.rotation
                    },
                painter = icon.painter,
                contentDescription = null
            )
        }
    }
}

@AppPreview
@Composable
private fun EmptyDecorationsPreview() {
    PreviewContainer {
        EmptyDecorations(
            modifier = Modifier.fillMaxSize()
        )
    }
}