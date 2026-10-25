package com.grippo.wheel.picker.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
internal actual fun rememberWheelTick(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) {
        return@remember { haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick) }
    }
}
