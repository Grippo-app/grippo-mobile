package com.grippo.network.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
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

public class NetworkClient(
    httpClient: HttpClient,
    json: Json
) {

    private val clientProvider = httpClient.config {
        install(Logging) {
            level = LogLevel.ALL
            logger = ClientLogger
        }

        install(ContentNegotiation) {
            json(
                json = json,
                contentType = ContentType.Application.Json
            )
        }

        defaultRequest {
            host = "192.168.0.105:3010"
            url { protocol = URLProtocol.HTTP }
//            url { protocol = URLProtocol.HTTPS }
            contentType(ContentType.Application.Json)
        }
    }

    public suspend fun invoke(
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
