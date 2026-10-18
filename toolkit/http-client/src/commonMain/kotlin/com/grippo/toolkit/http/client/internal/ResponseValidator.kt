package com.grippo.toolkit.http.client.internal

import com.grippo.core.error.provider.AppError
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

internal fun HttpClientConfig<*>.responseValidator(
    apiErrorParser: ApiErrorParser
) = HttpResponseValidator {
    validateResponse { response ->
        val statusCode = response.status.value

        if (statusCode in 200..299) return@validateResponse

        var bodyReadFailure: IOException? = null
        val rawBody = try {
            response.bodyAsText()
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: IOException) {
            cause.rethrowCancellationOrFatal()
            bodyReadFailure = cause
            null
        }

        when (statusCode) {
            in 400..499 -> {
                val parsed = apiErrorParser.parseDetailedMessage(rawBody = rawBody, status = statusCode)
                throw AppError.Network.Expected(
                    keys = apiErrorParser.parseKeys(rawBody),
                    title = parsed.title,
                    description = parsed.description,
                    cause = bodyReadFailure,
                )
            }

            in 500..599 -> {
                throw AppError.Network.Unexpected(
                    message = apiErrorParser.getDefaultServerErrorMessage(statusCode),
                    cause = bodyReadFailure,
                )
            }

            else -> throw AppError.Network.Unexpected(
                message = "Unexpected HTTP code: $statusCode",
                cause = bodyReadFailure,
            )
        }
    }

    handleResponseExceptionWithRequest { cause, _ ->
        if (cause !is Exception || cause is CancellationException) {
            throw cause
        }

        if (cause is JsonConvertException) {
            cause.rethrowCancellationOrFatal()
        }

        throw when (cause) {
            is AppError -> cause

            is HttpRequestTimeoutException -> AppError.Network.Timeout(
                message = "Request timed out. Try again.",
                cause = cause
            )

            is JsonConvertException -> AppError.Network.Unexpected(
                message = "Invalid server response format.",
                cause = cause,
            )

            is IOException -> AppError.Network.NoInternet(
                message = "Connection lost or unavailable.",
                cause = cause
            )

            else -> AppError.Network.Unexpected(
                message = cause.message ?: "Unexpected network error",
                cause = cause,
            )
        }
    }
}
