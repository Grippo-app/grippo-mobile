package com.grippo.services.google.auth

public expect class GoogleAuthUiProvider {
    public suspend fun signIn(): Result<GoogleAccount>
}
