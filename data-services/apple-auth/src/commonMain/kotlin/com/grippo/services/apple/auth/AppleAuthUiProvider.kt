package com.grippo.services.apple.auth

public expect class AppleAuthUiProvider {
    public suspend fun signIn(): AppleAccount
}
