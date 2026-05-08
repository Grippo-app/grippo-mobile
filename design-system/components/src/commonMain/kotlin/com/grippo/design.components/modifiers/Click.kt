package com.grippo.design.components.modifiers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

@Composable
public fun Modifier.scalableClick(
    enabled: Boolean = true,
    haptic: Boolean = false,
    role: Role? = Role.Button,
    onClick: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this

    val scaleDown = 0.97f
    val minPressDuration = 100.milliseconds
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val hapticEnabled by rememberUpdatedState(haptic)
    var pressed by remember { mutableStateOf(false) }

    val scale = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            scale.animateTo(
                targetValue = scaleDown,
                animationSpec = tween(durationMillis = 80, easing = FastOutLinearInEasing)
            )
        } else {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing)
            )
        }
    }

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(Unit) {
            var releaseJob: Job? = null
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                releaseJob?.cancel()
                if (hapticEnabled) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                pressed = true
                val pressMark = TimeSource.Monotonic.markNow()
                val up = waitForUpOrCancellation()
                if (up != null) {
                    val elapsed = pressMark.elapsedNow()
                    if (elapsed < minPressDuration) {
                        releaseJob = coroutineScope.launch {
                            delay(minPressDuration - elapsed)
                            pressed = false
                        }
                    } else {
                        pressed = false
                    }
                } else {
                    pressed = false
                }
            }
        }
        .clickable(
            interactionSource = null,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}
