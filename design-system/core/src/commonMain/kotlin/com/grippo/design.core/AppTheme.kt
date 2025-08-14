package com.grippo.design.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.grippo.design.core.internal.ProvideResources

@Composable
public fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    ProvideResources(
        darkTheme = darkTheme,
        content = content,
    )
}
