package com.grippo.home.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.barbell
import com.grippo.design.resources.provider.box
import com.grippo.design.resources.provider.dumbbell
import com.grippo.design.resources.provider.home_empty_subtitle
import com.grippo.design.resources.provider.home_empty_title
import com.grippo.design.resources.provider.plate
import com.grippo.design.resources.provider.start_workout
import com.grippo.design.resources.provider.weight
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun EmptyHomeContent(
    modifier: Modifier,
    onStartTraining: () -> Unit
) {
    val title = AppTokens.strings.res(Res.string.home_empty_title)
    val subtitle = AppTokens.strings.res(Res.string.home_empty_subtitle)

    Box(modifier = modifier) {
        EmptyHomeDecorations(
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = AppTokens.typography.h2(),
                color = AppTokens.colors.text.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

            Text(
                text = subtitle,
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Button(
                modifier = Modifier.fillMaxWidth(),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.start_workout),
                ),
                style = ButtonStyle.Primary,
                onClick = onStartTraining
            )
        }
    }
}

@Composable
private fun EmptyHomeDecorations(modifier: Modifier = Modifier) {
    val largeIconSize = AppTokens.dp.home.empty.image
    val dumbbell = AppTokens.drawables.res(Res.drawable.dumbbell)
    val barbell = AppTokens.drawables.res(Res.drawable.barbell)
    val plate = AppTokens.drawables.res(Res.drawable.plate)
    val box = AppTokens.drawables.res(Res.drawable.box)
    val weight = AppTokens.drawables.res(Res.drawable.weight)

    val decorations = remember {
        persistentListOf(
            EmptyHomeIcon(
                painter = dumbbell,
                alignment = Alignment.TopStart,
                offset = DpOffset(-(largeIconSize / 3), -(34.dp)),
                rotation = -12f,
                size = largeIconSize,
            ),
            EmptyHomeIcon(
                painter = barbell,
                alignment = Alignment.TopEnd,
                offset = DpOffset((largeIconSize / 3), (52).dp),
                rotation = 0f,
                size = largeIconSize * 1.3f,
            ),
            EmptyHomeIcon(
                painter = plate,
                alignment = Alignment.CenterStart,
                offset = DpOffset(-(largeIconSize / 2), -(34.dp)),
                rotation = -18f,
                size = largeIconSize,
            ),
            EmptyHomeIcon(
                painter = box,
                alignment = Alignment.BottomEnd,
                offset = DpOffset((largeIconSize / 3), -(52).dp),
                rotation = -8f,
                size = largeIconSize,
            ),
            EmptyHomeIcon(
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

@Immutable
private data class EmptyHomeIcon(
    val painter: Painter,
    val alignment: Alignment,
    val offset: DpOffset,
    val rotation: Float,
    val size: Dp,
)