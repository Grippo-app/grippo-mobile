package com.grippo.design.resources

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

public object AppDrawable {

    @Composable
    public fun res(value: DrawableResource): Painter {
        return painterResource(value)
    }
}