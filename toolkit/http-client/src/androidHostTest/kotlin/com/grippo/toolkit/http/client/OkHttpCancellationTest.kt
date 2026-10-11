package com.grippo.toolkit.http.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertTrue

class OkHttpCancellationTest {
    @Test
    fun cancellingStreamingResponsesDoesNotThrowFromCompletionHandlers() = runBlocking {
        val requestCount = 30
        val started = CountDownLatch(requestCount)
        val release = CountDownLatch(1)
        val connections = CopyOnWriteArrayList<Socket>()
        val failures = CopyOnWriteArrayList<Throwable>()
        val server = ServerSocket(0, requestCount, InetAddress.getLoopbackAddress())
        val acceptor = thread(isDaemon = true) {
            repeat(requestCount) {
                val socket = server.accept()
                connections.add(socket)
                thread(isDaemon = true) {
                    socket.use {
                        val input = it.getInputStream().bufferedReader()
                        while (!input.readLine().isNullOrEmpty()) { }
                        val output = it.getOutputStream()
                        output.write("HTTP/1.1 200 OK\r\nContent-Length: 1048576\r\nConnection: close\r\n\r\npartial".toByteArray())
                        output.flush()
                        started.countDown()
                        release.await(15, TimeUnit.SECONDS)
                    }
                }
            }
        }
        val client = HttpClient(OkHttp)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error -> failures.add(error) })
        try {
            val jobs = List(requestCount) {
                scope.launch { client.get("http://127.0.0.1:${server.localPort}/image").bodyAsText() }
            }
            withTimeout(10_000) {
                while (started.count > 0) delay(10)
            }
            jobs.forEach { it.cancel() }
            withTimeout(10_000) { jobs.forEach { it.join() } }
            assertTrue(jobs.all { it.isCancelled })
            assertTrue(failures.isEmpty(), failures.joinToString())
        } finally {
            scope.cancel()
            client.close()
            release.countDown()
            connections.forEach { it.close() }
            server.close()
            acceptor.join(1000)
        }
    }
}
