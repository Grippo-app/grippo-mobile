package com.grippo.network.client

import com.grippo.network.internal.ApiErrorParser
import com.grippo.network.internal.TokenProvider
import com.grippo.network.internal.responseValidator
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth

internal fun HttpClient.configureGrippo(
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
