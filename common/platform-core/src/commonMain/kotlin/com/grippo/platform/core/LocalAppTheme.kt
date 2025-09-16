package com.grippo.platform.core

import androidx.compose.runtime.Composable

public expect object LocalAppTheme {
    public val current: Boolean
        @Composable get

    public fun system(): Boolean
}