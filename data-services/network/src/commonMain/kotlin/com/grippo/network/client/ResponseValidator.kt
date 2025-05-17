package com.grippo.network.client

import com.grippo.error.provider.AppError
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.TimeoutCancellationException

internal fun HttpClientConfig<*>.responseValidator(
    apiErrorParser: ApiErrorParser
) = HttpResponseValidator {
    validateResponse { response: HttpResponse ->
        val statusCode = response.status.value

        when (statusCode) {
            in 100..199 -> Unit
            in 200..299 -> Unit

            in 300..399 -> throw AppError.Network.Unexpected(
                message = "Unexpected redirect [$statusCode]"
            )

            in 400..499 -> {
                val rawBody = runCatching { response.bodyAsText() }.getOrNull()

                throw AppError.Network.Expected(
                    keys = apiErrorParser.parseKeys(rawBody),
                    message = apiErrorParser.parseMessage(rawBody)
                )
            }

            in 500..599 -> throw AppError.Network.Unexpected(
                message = apiErrorParser.getDefaultServerErrorMessage(statusCode)
            )

            else -> throw AppError.Network.Unexpected(
                message = "Unexpected HTTP code: $statusCode"
            )
        }
    }

    handleResponseExceptionWithRequest { cause, _ ->
        throw when (cause) {
            is RedirectResponseException,
            is ClientRequestException,
            is ServerResponseException -> {
                // already handled in validateResponse — but just in case
                AppError.Network.Unexpected(
                    message = cause.message ?: "Unexpected HTTP exception",
                    cause = cause
                )
            }

            is TimeoutCancellationException -> AppError.Network.Timeout(
                message = "Request timed out. Try again.",
                cause = cause
            )

            is ResponseException -> AppError.Network.Unexpected(
                message = cause.message ?: "Network error",
                cause = cause
            )

            else -> AppError.Network.ConnectionLost(
                message = cause.message ?: "Network connection lost.",
                cause = cause
            )
        }
    }
}