package com.grippo.wheel.picker

import androidx.compose.foundation.BorderStroke
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

    @Composable
    public fun border(): State<BorderStroke>
}

@Immutable
public class DefaultSelectorProperties(
    private val shape: Shape,
    private val color: Color,
    private val border: BorderStroke
) : SelectorProperties {

    @Composable
    override fun shape(): State<Shape> {
        return rememberUpdatedState(shape)
    }

    @Composable
    override fun color(): State<Color> {
        return rememberUpdatedState(color)
    }

    @Composable
    override fun border(): State<BorderStroke> {
        return rememberUpdatedState(border)
    }
}