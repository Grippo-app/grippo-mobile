package com.grippo.services.apple.auth

import com.grippo.toolkit.context.NativeContext
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

public actual class AppleAuthProvider actual constructor(
    @Suppress("UNUSED_PARAMETER") nativeContext: NativeContext,
    @Suppress("UNUSED_PARAMETER") httpClient: HttpClient,
    @Suppress("UNUSED_PARAMETER") json: Json,
) {
    public actual val isSupported: Boolean
        get() = false

    public actual fun getUiProvider(context: AppleAuthUiContext): AppleAuthUiProvider {
        throw AppleAuthException("Apple sign-in is not supported on Android")
    }

    public actual suspend fun signOut() {
        return
    }
}
