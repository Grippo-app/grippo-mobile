package com.grippo.services.google.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
public actual fun rememberGoogleAuthUiContext(): GoogleAuthUiContext? {
    val context = LocalContext.current
    val activity = remember(context) { findActivity(context) }
    return remember(activity) { activity?.let { googleAuthUiContext(it) } }
}

private tailrec fun findActivity(context: Context): Activity? {
    return when (context) {
        is Activity -> context
        is ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }
}
