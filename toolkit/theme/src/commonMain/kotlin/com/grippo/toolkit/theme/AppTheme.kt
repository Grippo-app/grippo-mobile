package com.grippo.toolkit.theme

import androidx.compose.runtime.Composable

public expect object AppTheme {
    public val current: Boolean
        @Composable get

    public fun current(): Boolean
}
