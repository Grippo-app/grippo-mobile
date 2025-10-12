package com.grippo.http.client.internal

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Single

@Single
internal class ApiErrorParser(private val json: Json) {

    companion object {
        private const val MAX_DEPTH = 4
        private const val DEFAULT_ERROR = "Something went wrong. Please try again."
    }

    data class ParsedError(val title: String, val description: String?)

    fun parseDetailedMessage(rawBody: String?, status: Int?): ParsedError {
        if (rawBody.isNullOrBlank()) {
            return ParsedError(
                title = getFallbackMessage(status),
                description = null
            )
        }

        return try {
            val element = runCatching { json.parseToJsonElement(rawBody) }
                .getOrElse {
                    return ParsedError(
                        title = getFallbackMessage(status),
                        description = null
                    )
                }

            if (element is JsonObject) {
                val message = element["message"]?.jsonPrimitive?.contentOrNull
                val error = element["error"]?.jsonPrimitive?.contentOrNull
                val reason = element["reason"]?.jsonPrimitive?.contentOrNull
                val description = element["description"]?.jsonPrimitive?.contentOrNull

                val main = message ?: description ?: reason
                val secondary = error

                val title = main?.takeIf { it.isNotBlank() } ?: getFallbackMessage(status)
                val desc = secondary?.takeIf { it.isNotBlank() }

                return ParsedError(title = title, description = desc)
            }

            val fallbackText = extractMessagesRecursive(element)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("\n")
                .ifBlank { getFallbackMessage(status) }

            ParsedError(title = fallbackText, description = null)
        } catch (e: Exception) {
            ParsedError(title = getFallbackMessage(status), description = null)
        }
    }

    fun parseKeys(rawBody: String?): List<String> {
        if (rawBody.isNullOrBlank()) return emptyList()

        return runCatching {
            val element = json.parseToJsonElement(rawBody)
            element.jsonObject["errors"]
                ?.jsonArray
                ?.mapNotNull { it.jsonObject["code"]?.jsonPrimitive?.content }
                .orEmpty()
        }.getOrDefault(emptyList())
    }

    fun getDefaultClientErrorMessage(status: Int?): String = when (status) {
        400 -> "Invalid request. Please try again."
        401 -> "You’re not authorized."
        403 -> "Access denied."
        404 -> "Not found."
        408 -> "Request timed out."
        429 -> "Too many requests. Please wait."
        else -> "Something went wrong on your side."
    }

    fun getDefaultServerErrorMessage(status: Int?): String = when (status) {
        500 -> "Server error. Try again later."
        502 -> "Bad gateway."
        503 -> "Service unavailable."
        504 -> "Gateway timeout."
        else -> "Server issue occurred."
    }

    private fun getFallbackMessage(status: Int?): String {
        return when (status) {
            in 400..499 -> getDefaultClientErrorMessage(status)
            in 500..599 -> getDefaultServerErrorMessage(status)
            else -> DEFAULT_ERROR
        }
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