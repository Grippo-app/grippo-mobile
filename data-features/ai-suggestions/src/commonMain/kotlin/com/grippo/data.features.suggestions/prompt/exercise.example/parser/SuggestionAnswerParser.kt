package com.grippo.data.features.suggestions.prompt.exercise.example.parser

import com.grippo.data.features.api.suggestion.models.ExerciseExampleSuggestion
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

/**
 * Sanitizes and parses LLM responses, ensuring the chosen exercise id is valid
 * and the reason text fits platform constraints.
 */
@Single
internal class SuggestionAnswerParser(
    private val json: Json
) {

    fun parse(raw: String, allowedIds: Set<String>): ExerciseExampleSuggestion? {
        val parsed = parse(raw) ?: return null
        return if (allowedIds.contains(parsed.id)) parsed else null
    }

    fun normalizeSuggestionOrNull(
        suggestion: ExerciseExampleSuggestion
    ): ExerciseExampleSuggestion? {
        val id = suggestion.id.trim()
        var reason = suggestion.reason.trim().replace(Regex("\\s+"), " ")
        if (id.isEmpty() || reason.isEmpty()) return null
        if (reason.length > 500) reason = reason.take(500)
        return suggestion.copy(id = id, reason = reason)
    }

    private fun parse(raw: String): ExerciseExampleSuggestion? {
        val sanitized = sanitizeRawModelText(raw)
        val jsonText = extractFirstJsonObject(sanitized) ?: return null

        val ans = runCatching { json.decodeFromString<ModelAnswer>(jsonText) }.getOrNull()
            ?: run {
                val id = Regex("\"exerciseExampleId\"\\s*:\\s*\"([^\"]+)\"")
                    .find(jsonText)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                val reason = Regex("\"reason\"\\s*:\\s*\"([\\s\\S]*?)\"\\s*[,}]")
                    .find(jsonText)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                if (id.isNotEmpty() && reason.isNotEmpty()) ModelAnswer(id, reason) else null
            }
            ?: return null

        val id = ans.exerciseExampleId.trim()
        val reason = ans.reason.trim()
        if (id.isEmpty() || reason.isEmpty()) return null

        return ExerciseExampleSuggestion(id = id, reason = reason)
    }

    private fun sanitizeRawModelText(raw: String): String {
        return raw
            .replace("```json", "```")
            .replace("```", "")
            .replace('“', '"')
            .replace('”', '"')
            .replace('’', '\'')
            .trim()
    }

    private fun extractFirstJsonObject(input: String): String? {
        var depth = 0
        var start = -1
        for (i in input.indices) {
            when (input[i]) {
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }

                '}' -> {
                    if (depth > 0) {
                        depth--
                        if (depth == 0 && start >= 0) {
                            return input.substring(start, i + 1)
                        }
                    }
                }
            }
        }
        return null
    }

    @Serializable
    private data class ModelAnswer(
        val exerciseExampleId: String,
        val reason: String
    )
}
