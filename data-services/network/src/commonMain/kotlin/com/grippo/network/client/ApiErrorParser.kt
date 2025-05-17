package com.grippo.network.client

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class ApiErrorParser(private val json: Json) {

    companion object {
        private const val MAX_DEPTH = 4
    }

    fun parseMessage(rawBody: String?): String {
        if (rawBody.isNullOrBlank()) return "Something went wrong"

        return runCatching {
            val element = json.parseToJsonElement(rawBody)
            extractMessagesRecursive(element)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("\n")
                .ifBlank { "Something went wrong" }
        }.getOrElse { "Something went wrong" }
    }

    fun parseKeys(rawBody: String?): List<String> {
        if (rawBody.isNullOrBlank()) return emptyList()

        return runCatching {
            val element = Json.parseToJsonElement(rawBody)
            element.jsonObject["errors"]
                ?.jsonArray
                ?.mapNotNull { it.jsonObject["code"]?.jsonPrimitive?.content }
                .orEmpty()
        }.getOrDefault(emptyList())
    }

    fun getDefaultClientErrorMessage(status: Int): String = when (status) {
        400 -> "Invalid request. Please try again."
        401 -> "You’re not authorized."
        403 -> "Access denied."
        404 -> "Not found."
        408 -> "Request timed out."
        429 -> "Too many requests. Please wait."
        else -> "Something went wrong on your side."
    }

    fun getDefaultServerErrorMessage(status: Int): String = when (status) {
        500 -> "Server error. Try again later."
        502 -> "Bad gateway."
        503 -> "Service unavailable."
        504 -> "Gateway timeout."
        else -> "Server issue occurred."
    }

    private fun extractMessagesRecursive(
        json: JsonElement,
        depth: Int = 0
    ): List<String> {
        if (depth > MAX_DEPTH) return emptyList()

        return when (json) {
            is JsonPrimitive -> if (json.isString) listOf(json.content) else emptyList()
            is JsonArray -> json.flatMap { extractMessagesRecursive(it, depth + 1) }
            is JsonObject -> {
                val directMessages = buildList {
                    listOf("message", "description", "reason", "error").forEach { key ->
                        json[key]?.let { addAll(extractMessagesRecursive(it, depth + 1)) }
                    }
                }

                val nestedMessages = json.values.flatMap { extractMessagesRecursive(it, depth + 1) }
                directMessages + nestedMessages
            }
        }
    }
}
