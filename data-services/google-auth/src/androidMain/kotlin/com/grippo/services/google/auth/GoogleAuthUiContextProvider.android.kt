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
    val activity = remember(context) { context.findActivity() }
    return remember(activity) { activity?.let { googleAuthUiContext(it) } }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
