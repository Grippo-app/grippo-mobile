package com.grippo.toolkit.http.client

import com.grippo.toolkit.context.NativeContext
import io.ktor.client.HttpClient

internal expect fun NativeContext.driver(): HttpClient