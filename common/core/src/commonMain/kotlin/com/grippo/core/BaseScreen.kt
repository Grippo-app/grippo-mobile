package com.grippo.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.painter.Painter as ComposePainter

@Stable
public sealed interface ScreenBackground {
    @Immutable
    public data class Color(val value: ComposeColor) : ScreenBackground

    @Immutable
    public data class Painter(val value: ComposePainter) : ScreenBackground
}

@Composable
public fun BaseComposeScreen(
    background: ScreenBackground,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {

        when (background) {
            is ScreenBackground.Color -> Box(
                modifier = Modifier.fillMaxSize().background(background.value),
            )

            is ScreenBackground.Painter -> androidx.compose.foundation.Image(
                modifier = Modifier.fillMaxSize(),
                painter = background.value,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { focusManager.clearFocus(force = true) }
                ),
            content = content
        )
    }
}