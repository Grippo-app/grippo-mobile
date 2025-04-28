package com.grippo.network

import com.grippo.platform.core.NativeContext
import io.ktor.client.HttpClient

internal expect fun NativeContext.driver(): HttpClient