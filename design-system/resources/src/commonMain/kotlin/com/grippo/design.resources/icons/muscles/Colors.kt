package com.grippo.design.resources.icons.muscles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color

public interface MuscleColorProperties {
    @Composable
    public fun muscleColor(): State<Color>

    @Composable
    public fun backgroundColor(): State<Color>

    @Composable
    public fun defaultFrontColor(): State<Color>

    @Composable
    public fun defaultBackColor(): State<Color>

    @Composable
    public fun backgroundBackColor(): State<Color>

    @Composable
    public fun backgroundFrontColor(): State<Color>

    @Composable
    public fun outlineColor(): State<Color>
}

@Immutable
public class DefaultMuscleColorProperties(
    public val muscle: Color,
    public val background: Color,
    public val defaultFront: Color,
    public val defaultBack: Color,
    public val backgroundFront: Color,
    public val backgroundBack: Color,
    public val outline: Color,
) : MuscleColorProperties {
    @Composable
    override fun backgroundColor(): State<Color> = rememberUpdatedState(background)

    @Composable
    override fun muscleColor(): State<Color> = rememberUpdatedState(muscle)

    @Composable
    override fun defaultFrontColor(): State<Color> = rememberUpdatedState(defaultFront)

    @Composable
    override fun defaultBackColor(): State<Color> = rememberUpdatedState(defaultBack)

    @Composable
    override fun backgroundBackColor(): State<Color> = rememberUpdatedState(backgroundBack)

    @Composable
    override fun backgroundFrontColor(): State<Color> = rememberUpdatedState(backgroundFront)

    @Composable
    override fun outlineColor(): State<Color> = rememberUpdatedState(outline)
}