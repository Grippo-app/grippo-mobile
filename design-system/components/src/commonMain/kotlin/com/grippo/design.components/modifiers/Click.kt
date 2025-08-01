package com.grippo.design.components.modifiers

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

@Composable
public fun Modifier.scalableClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {

    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val scaleDown = 0.97f
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> pressed = true
                is PressInteraction.Release, is PressInteraction.Cancel -> pressed = false
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        label = "clickScale"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            enabled = enabled,
            indication = null,
            onClick = onClick
        )
}