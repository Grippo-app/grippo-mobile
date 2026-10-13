package com.grippo.toolkit.http.client.internal

import com.grippo.core.error.provider.AppError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ResponseValidatorTest {
    @Test
    fun cancellationWhileReadingAnErrorBodyIsNotAValidationError() = runTest {
        val cancellation = CancellationException("screen closed")
        val body = ByteChannel().apply { cancel(cancellation) }
        val client = HttpClient(MockEngine { respond(body, HttpStatusCode.BadRequest) }) {
            responseValidator(ApiErrorParser(Json))
        }
        try {
            assertFailsWith<CancellationException> { client.get("https://example.test/error") }
        } finally {
            client.close()
        }
    }

    @Test
    fun anOuterTimeoutRemainsCoroutineCancellation() = runTest {
        val client = HttpClient(MockEngine { withTimeout(1) { awaitCancellation() } }) {
            responseValidator(ApiErrorParser(Json))
        }
        try {
            assertFailsWith<CancellationException> { client.get("https://example.test/slow") }
        } finally {
            client.close()
        }
    }

    @Test
    fun anUnknownTransportFailurePreservesItsCause() = runTest {
        val failure = IllegalStateException("transport failure")
        val client = HttpClient(MockEngine { throw failure }) {
            responseValidator(ApiErrorParser(Json))
        }
        try {
            val error = assertFailsWith<AppError.Network.Unexpected> {
                client.get("https://example.test/error")
            }
            assertSame(failure, error.cause)
        } finally {
            client.close()
        }
    }

    @Test
    fun fatalAndNonExceptionTransportFailuresPropagateUnchanged() = runTest {
        for (original in listOf(Error("fatal transport failure"), Throwable("non-Exception transport failure"))) {
            val client = HttpClient(MockEngine { throw original }) {
                responseValidator(ApiErrorParser(Json))
            }
            try {
                val observed = assertFailsWith<Throwable> {
                    client.get("https://example.test/error")
                }
                assertSame(original, observed)
            } finally {
                client.close()
            }
        }
    }
}
