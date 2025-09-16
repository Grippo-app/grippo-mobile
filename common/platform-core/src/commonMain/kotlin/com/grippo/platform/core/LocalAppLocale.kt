package com.grippo.platform.core

import androidx.compose.runtime.Composable

// Expect: platform-specific actuals will be provided on Android/iOS.
public expect object LocalAppLocale {
    /**
     * Effective current app locale tag (BCP-47, e.g., "uk-UA").
     * If no override provided, follows the system/app configuration.
     */
    public val current: String
        @Composable get

    /**
     * Pure system default locale tag (BCP-47).
     */
    public fun system(): String
}