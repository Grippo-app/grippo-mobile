package com.grippo.toolkit.localization

import androidx.compose.runtime.Composable

public expect object AppLocale {

    public val current: String
        @Composable get

    public fun current(): String
}
