package com.grippo.network.client

import com.grippo.network.internal.ApiErrorParser
import com.grippo.network.internal.responseValidator
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import kotlinx.coroutines.TimeoutCancellationException

internal fun HttpClient.configureKoog(
    apiKey: String,
    apiErrorParser: ApiErrorParser,
) = this.config {

    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 30_000
    }

    install(Auth) {
        bearer {
            loadTokens { BearerTokens(apiKey, "") }
            sendWithoutRequest { true }
        }
    }

    // Retries for 429/5xx and transient I/O errors
    install(io.ktor.client.plugins.HttpRequestRetry) {
        maxRetries = 5
        retryIf { _, response ->
            val code = response.status.value
            code == 429 || code == 408 || code == 425 || code in 500..599
        }
        retryOnExceptionIf { _, cause ->
            // KMP-safe network/timeout conditions
            cause is io.ktor.utils.io.errors.IOException ||
                    cause is HttpRequestTimeoutException ||
                    cause is TimeoutCancellationException ||
                    cause is io.ktor.util.network.UnresolvedAddressException
        }
        delayMillis { attempt ->
            val base = 500L * (1 shl attempt.coerceAtMost(6))
            base + kotlin.random.Random.nextLong(0L, 250L)
        }
    }

    responseValidator(apiErrorParser)
}
