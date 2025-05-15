package com.grippo.network.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth

internal fun HttpClient.configure(
    tokenProvider: TokenProvider
) = this.config {

    install(HttpTimeout) {
        requestTimeoutMillis = 35_000
        connectTimeoutMillis = 35_000
        socketTimeoutMillis = 35_000
    }


    install(Auth) {
        providers.add(tokenProvider)
    }

    responseValidator()
}
