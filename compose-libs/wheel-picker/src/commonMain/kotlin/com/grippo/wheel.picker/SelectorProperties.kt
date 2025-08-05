package com.grippo.wheel.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

public interface SelectorProperties {
    @Composable
    public fun shape(): State<Shape>

    @Composable
    public fun color(): State<Color>
}

@Immutable
public class DefaultSelectorProperties(
    private val shape: Shape,
    private val color: Color,
) : SelectorProperties {

    @Composable
    override fun shape(): State<Shape> {
        return rememberUpdatedState(shape)
    }

    @Composable
    override fun color(): State<Color> {
        return rememberUpdatedState(color)
    }
}