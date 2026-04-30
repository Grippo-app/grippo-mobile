package com.grippo.core.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color as ComposeColor

@Stable
public sealed interface ScreenBackground {
    @Immutable
    public data class Color(val value: ComposeColor) : ScreenBackground
}

@Composable
public fun BaseComposeScreen(
    background: ScreenBackground.Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .background(background.value)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus(force = true) })
            },
        content = content
    )
}
