package com.grippo.services.google.auth

import android.app.Activity
import android.content.Context

internal class AndroidGoogleAuthUiContext(
    val activity: Activity,
) : GoogleAuthUiContext

public fun googleAuthUiContext(activity: Activity): GoogleAuthUiContext {
    return AndroidGoogleAuthUiContext(activity)
}

internal fun GoogleAuthUiContext.asAndroidActivity(): Activity {
    val activity = (this as? AndroidGoogleAuthUiContext)?.activity
        ?: throw GoogleAuthException.ProviderMisconfigured(
            message = "GoogleAuthUiContext must be created with an Android Activity context",
        )

    if (activity.isFinishing) {
        throw GoogleAuthException.Interrupted(
            message = "Google sign-in cannot start because Activity is finishing",
        )
    }

    if (activity.isDestroyed) {
        throw GoogleAuthException.Interrupted(
            message = "Google sign-in cannot start because Activity is destroyed",
        )
    }

    return activity
}

internal fun GoogleAuthUiContext.asAndroidContext(): Context {
    return asAndroidActivity()
}
