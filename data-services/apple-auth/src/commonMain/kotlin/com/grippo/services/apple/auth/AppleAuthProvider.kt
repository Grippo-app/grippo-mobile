package com.grippo.services.apple.auth

public expect class AppleAuthProvider() {
    public val isSupported: Boolean
    public fun getUiProvider(context: AppleAuthUiContext): AppleAuthUiProvider
    public suspend fun signOut()
}
