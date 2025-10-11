package com.grippo.network.internal

import com.grippo.error.provider.AppError
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.io.IOException

internal fun HttpClientConfig<*>.responseValidator(
    apiErrorParser: ApiErrorParser
) = HttpResponseValidator {
    validateResponse { response ->
        val statusCode = response.status.value

        if (statusCode in 200..299) return@validateResponse

        val rawBody = runCatching { response.bodyAsText() }.getOrNull()

        when (statusCode) {
            in 400..499 -> {
                val parsed = apiErrorParser.parseDetailedMessage(rawBody, statusCode)
                throw AppError.Network.Expected(
                    keys = apiErrorParser.parseKeys(rawBody),
                    title = parsed.title,
                    description = parsed.description
                )
            }

            in 500..599 -> {
                throw AppError.Network.Unexpected(
                    message = apiErrorParser.getDefaultServerErrorMessage(statusCode)
                )
            }

            else -> throw AppError.Network.Unexpected(
                message = "Unexpected HTTP code: $statusCode"
            )
        }
    }

    handleResponseExceptionWithRequest { cause, _ ->
        when (cause) {
            is AppError.Network.Expected -> throw cause

            is TimeoutCancellationException,
            is HttpRequestTimeoutException -> throw AppError.Network.Timeout(
                message = "Request timed out. Try again.",
                cause = cause
            )

            is JsonConvertException -> throw AppError.Network.Unexpected(
                message = "Invalid server response format.",
                cause = cause
            )

            is IOException -> throw AppError.Network.NoInternet(
                message = "Connection lost or unavailable.",
                cause = cause
            )

            else -> AppError.Network.Unexpected(
                message = cause.message ?: "Unexpected network error",
                cause = cause
            )
        }
    }
}