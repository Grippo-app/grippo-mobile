package com.grippo.services.google.auth

import android.app.Activity
import android.content.Context

internal class AndroidGoogleAuthUiContext(
    val activity: Activity,
) : GoogleAuthUiContext

public fun googleAuthUiContext(activity: Activity): GoogleAuthUiContext {
    return AndroidGoogleAuthUiContext(activity)
}

internal fun GoogleAuthUiContext.asAndroidContext(): Context {
    return (this as? AndroidGoogleAuthUiContext)?.activity
        ?: error("GoogleAuthUiContext must be created with an Android Activity context")
}
