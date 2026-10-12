package com.grippo.services.apple.auth

public actual class AppleAuthProvider actual constructor() {
    public actual val isSupported: Boolean
        get() = false

    public actual fun getUiProvider(context: AppleAuthUiContext): AppleAuthUiProvider {
        throw AppleAuthException("Apple sign-in is not supported on Android")
    }

    public actual suspend fun signOut() {
        return
    }
}
