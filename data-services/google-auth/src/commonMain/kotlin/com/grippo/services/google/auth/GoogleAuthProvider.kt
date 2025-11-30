package com.grippo.services.google.auth

import com.grippo.toolkit.context.NativeContext
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

public expect class GoogleAuthProvider(
    nativeContext: NativeContext,
    httpClient: HttpClient,
    json: Json,
) {
    public val isSupported: Boolean
    public fun getUiProvider(context: GoogleAuthUiContext): GoogleAuthUiProvider
    public suspend fun signOut()
}
