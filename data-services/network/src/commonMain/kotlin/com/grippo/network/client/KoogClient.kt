package com.grippo.network.client

import com.grippo.network.internal.ApiErrorParser
import com.grippo.network.internal.ClientLogger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.path
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
internal class KoogClient(
    httpClient: HttpClient,
    json: Json,
    clientLogger: ClientLogger,
    apiErrorParser: ApiErrorParser
) {
    private val clientProvider = httpClient
        .configureKoog(
            apiKey = "",
            apiErrorParser = apiErrorParser
        ).config {
            install(Logging) {
                level = LogLevel.ALL
                logger = clientLogger
            }

            install(ContentNegotiation) {
                json(
                    json = json,
                    contentType = ContentType.Application.Json
                )
            }

            defaultRequest {
                // Base for OpenRouter
                host = "openrouter.ai"
                url {
                    protocol = URLProtocol.HTTPS
                    // Keep base path to /api/v1 so request { path("...") } appends to it
                    encodedPath = "/api/v1"
                }
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)

                // Helpful metadata for OpenRouter (safe no-ops elsewhere)
                header("HTTP-Referer", "https://grippo-app.com")
                header("X-Title", "Grippo")
            }
        }

    suspend fun invoke(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        queryParams: Map<String, String>? = null,
        headers: Map<String, String>? = null
    ): HttpResponse {
        return clientProvider.request {
            this.method = method
            url {
                // If absolute URL is passed, take it as-is; otherwise append to /api/v1
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    takeFrom(path)
                } else {
                    val normalized = if (path.startsWith("/")) path else "/$path"
                    path(normalized)
                }
                queryParams?.forEach { (k, v) -> parameters.append(k, v) }
            }
            headers?.forEach { (k, v) -> header(k, v) }
            body?.let { setBody(it) }
        }
    }
}