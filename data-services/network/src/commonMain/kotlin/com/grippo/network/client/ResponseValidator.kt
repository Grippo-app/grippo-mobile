package com.grippo.network.client

import com.grippo.error.provider.AppError
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal fun HttpClientConfig<*>.responseValidator() = HttpResponseValidator {
    validateResponse { response: HttpResponse ->
        when (val statusCode = response.status.value) {
            in 400..499 -> {
                val error = runCatching {
                    Json.decodeFromString<ApiError>(response.bodyAsText())
                }.getOrNull()

                val msg = runCatching {
                    error
                        ?.errors
                        ?.joinToString { "${it.description}\n" }
                        ?: "Something went wrong"
                }.getOrNull()

                throw AppError.Network.Expected(
                    keys = error?.errors?.mapNotNull { it.code } ?: emptyList(),
                    message = msg ?: "Something went wrong"
                )
            }

            in 500..599 -> {
                throw AppError.Network.Unexpected(
                    statusCode = statusCode,
                    message = "Something went wrong"
                )
            }
        }
    }
}

@Serializable
private data class ApiError(
    @SerialName("errors")
    val errors: List<ApiErrorItem>? = listOf()
)

@Serializable
private data class ApiErrorItem(
    @SerialName("code")
    val code: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("field")
    val field: String? = null,
)
