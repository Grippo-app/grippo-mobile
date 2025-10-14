package com.grippo.toolkit.http.client

import com.grippo.toolkit.context.NativeContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

internal actual fun NativeContext.driver(): HttpClient {
    return HttpClient(Android)
}