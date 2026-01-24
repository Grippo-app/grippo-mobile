package com.grippo.services.apple.auth

public actual class AppleAuthUiProvider {
    public actual suspend fun signIn(): Result<AppleAccount> {
        return Result.failure(AppleAuthException("Apple sign-in is not supported on Android"))
    }
}
