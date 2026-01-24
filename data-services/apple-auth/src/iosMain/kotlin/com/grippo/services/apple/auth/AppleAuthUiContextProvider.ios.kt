package com.grippo.services.apple.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
public actual fun rememberAppleAuthUiContext(): AppleAuthUiContext? {
    return remember { appleAuthUiContext() }
}
