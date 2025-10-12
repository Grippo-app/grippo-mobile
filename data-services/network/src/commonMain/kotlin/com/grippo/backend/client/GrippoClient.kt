package com.grippo.backend.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
internal class GrippoClient(
    httpClient: HttpClient,
    tokenProvider: TokenProvider,
    json: Json,
) {
    private val clientProvider = httpClient.config {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }

        install(Auth) {
            providers.add(tokenProvider)
        }

        install(ContentNegotiation) {
            json(
                json = json,
                contentType = ContentType.Application.Json
            )
        }

        defaultRequest {
            host = "grippo-app.com"
            url { protocol = URLProtocol.HTTPS }
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun invoke(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        queryParams: Map<String, String>? = null
    ): HttpResponse {
        return clientProvider.request {
            url {
                path(path)
                queryParams?.forEach { (key, value) -> parameters.append(key, value) }
                body?.let { setBody(body) }
            }
            this.method = method
        }
    }
}
