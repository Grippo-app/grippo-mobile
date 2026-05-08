package com.grippo.wheel.picker.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
internal actual fun rememberWheelTick(): () -> Unit {
    // Compose Multiplatform's iOS actual maps `SegmentFrequentTick` to
    // `UISelectionFeedbackGenerator.selectionChanged()` — the same generator the system
    // `UIPickerView` uses for its scroll click. Going through `LocalHapticFeedback` keeps
    // us inside the framework's lifecycle (generator instance is reused, prepare() hooks
    // get a chance to run) instead of allocating our own `UISelectionFeedbackGenerator`.
    val haptic = LocalHapticFeedback.current
    return remember(haptic) {
        return@remember { haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick) }
    }
}
