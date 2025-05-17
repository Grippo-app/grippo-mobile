package com.grippo.network.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth

internal fun HttpClient.configure(
    tokenProvider: TokenProvider,
    apiErrorParser: ApiErrorParser
) = this.config {

    install(HttpTimeout) {
        requestTimeoutMillis = 10_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 10_000
    }

    install(Auth) {
        providers.add(tokenProvider)
    }

    responseValidator(apiErrorParser)
}
