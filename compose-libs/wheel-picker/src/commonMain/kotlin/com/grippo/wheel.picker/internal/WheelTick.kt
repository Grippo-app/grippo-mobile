package com.grippo.wheel.picker.internal

import androidx.compose.runtime.Composable

/**
 * Returns a callback that emits one platform-native scroll-tick haptic.
 *
 * iOS — `UISelectionFeedbackGenerator.selectionChanged()` (the same generator the system
 * `UIPickerView` uses).
 * Android — `HapticFeedbackConstants.SEGMENT_FREQUENT_TICK` on API 34+, falls back to
 * `CLOCK_TICK` on older devices so the experience is consistent across the supported range
 * (`minSdk = 26`).
 *
 * The returned callback is stable across recompositions: `remember`ed on the platform
 * dependency (view / haptic provider), so it's safe to use as a `LaunchedEffect` key.
 */
@Composable
internal expect fun rememberWheelTick(): () -> Unit
