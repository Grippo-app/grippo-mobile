package com.grippo.platform.http.client

import com.grippo.platform.context.NativeContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun NativeContext.driver(): HttpClient {
    return HttpClient(Darwin) { engine { configureRequest { setAllowsCellularAccess(true) } } }
}