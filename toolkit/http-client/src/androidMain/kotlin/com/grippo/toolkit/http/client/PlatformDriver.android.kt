package com.grippo.toolkit.http.client

import com.grippo.toolkit.context.NativeContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun NativeContext.driver(): HttpClient {
    // Avoid Android's HttpURLConnection response-close race when image requests are cancelled.
    return HttpClient(OkHttp)
}
