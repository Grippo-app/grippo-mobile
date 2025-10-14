package com.grippo.http.client

import com.grippo.platform.context.NativeContext
import io.ktor.client.HttpClient

internal expect fun NativeContext.driver(): HttpClient