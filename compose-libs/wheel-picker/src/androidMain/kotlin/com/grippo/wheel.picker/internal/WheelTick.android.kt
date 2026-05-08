package com.grippo.wheel.picker.internal

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun rememberWheelTick(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        // SEGMENT_FREQUENT_TICK is the dedicated frequent-scroll-tick constant added in
        // Android 14. Pre-14, the system silently no-ops on unknown constants, so on older
        // devices we fall back to CLOCK_TICK — the closest UX match available since API 21
        // and respected by the user's system haptic settings.
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        return@remember { view.performHapticFeedback(constant) }
    }
}
