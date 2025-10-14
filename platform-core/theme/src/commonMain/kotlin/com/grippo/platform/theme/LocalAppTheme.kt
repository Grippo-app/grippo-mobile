package com.grippo.platform.theme

import androidx.compose.runtime.Composable

public expect object LocalAppTheme {
    public val current: Boolean
        @Composable get

    public fun current(): Boolean
}
