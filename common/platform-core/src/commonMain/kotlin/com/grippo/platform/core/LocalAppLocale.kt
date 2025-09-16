package com.grippo.platform.core

import androidx.compose.runtime.Composable

// Expect: platform-specific actuals will be provided on Android/iOS.
public expect object LocalAppLocale {

    public val current: String
        @Composable get

    public fun current(): String
}