package com.grippo.services.apple.auth

import com.grippo.toolkit.context.NativeContext
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

public expect class AppleAuthProvider(
    nativeContext: NativeContext,
    httpClient: HttpClient,
    json: Json,
) {
    public val isSupported: Boolean
    public fun getUiProvider(context: AppleAuthUiContext): AppleAuthUiProvider
    public suspend fun signOut()
}
