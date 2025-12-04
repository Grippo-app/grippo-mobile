package com.grippo.services.google.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
public actual fun rememberGoogleAuthUiContext(): GoogleAuthUiContext? {
    return remember { googleAuthUiContext() }
}
