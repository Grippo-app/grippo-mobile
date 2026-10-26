package com.grippo.toolkit.localization.internal

import androidx.compose.runtime.Composable

internal expect object SystemLocale {
    val current: String
        @Composable get
    fun current(): String
    fun apply(tag: String?)
}
