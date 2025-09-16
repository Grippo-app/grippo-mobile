package com.grippo.design.core

import androidx.compose.runtime.Composable
import com.grippo.design.core.internal.ProvideResources

@Composable
public fun AppTheme(
    darkTheme: Boolean,
    localeTag: String,
    content: @Composable () -> Unit,
) {
    ProvideResources(
        darkTheme = darkTheme,
        localeTag = localeTag,
        content = content,
    )
}
