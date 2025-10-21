package com.grippo.toolkit.localization

import androidx.compose.runtime.Composable

// Expect: platform-specific actuals will be provided on Android/iOS.
public expect object AppLocale {

    public val current: String
        @Composable get

    public fun current(): String
}
