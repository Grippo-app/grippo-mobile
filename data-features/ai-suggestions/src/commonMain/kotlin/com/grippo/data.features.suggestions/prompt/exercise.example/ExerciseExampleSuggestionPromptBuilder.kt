package com.grippo.data.features.suggestions.prompt.exercise.example

import com.grippo.ai.agent.AiAgentApi
import com.grippo.core.error.provider.AppError
import com.grippo.data.features.api.suggestion.models.ExerciseExampleSuggestion
import com.grippo.data.features.suggestions.prompt.exercise.example.catalog.ExampleCatalogLoader
import com.grippo.data.features.suggestions.prompt.exercise.example.parser.SuggestionAnswerParser
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.SystemSection
import com.grippo.data.features.suggestions.prompt.exercise.example.selection.CandidateSelector
import com.grippo.data.features.suggestions.prompt.exercise.example.signals.PredictionSignalsBuilder
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.localization.AppLocale
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Single

/**
 * Coordinates catalog hydration, signal building, candidate ranking, and AI parsing
 * to deliver a validated exercise example suggestion.
 */
@Single
internal class ExerciseExampleSuggestionPromptBuilder(
    private val aiAgent: AiAgentApi,
    private val catalogLoader: ExampleCatalogLoader,
    private val signalsBuilder: PredictionSignalsBuilder,
    private val candidateSelector: CandidateSelector,
    private val answerParser: SuggestionAnswerParser,
    private val systemPromptBuilder: SystemSection,
) {

    suspend fun suggest(now: LocalDateTime = DateTimeUtils.now()): Result<ExerciseExampleSuggestion?> {
        val locale = AppLocale.current()

        val catalog = catalogLoader.load() ?: return Result.failure(
            AppError.Expected(
                "Invalid example catalog",
                description = null
            )
        )

        val signals = signalsBuilder.build(now, catalog)

        val candidates = candidateSelector.select(catalog, signals, now)

        if (candidates.isEmpty()) return Result.failure(
            AppError.Expected(
                "Candidates are empty",
                description = null
            )
        )

        val prompt = ExerciseExamplePromptComposer(
            now = now,
            signals = signals,
            candidates = candidates,
            locale = locale
        ).compose()

        val systemPrompt = systemPromptBuilder.build()

        val answer = aiAgent.ask(prompt, systemPrompt)

        return answer.map { raw ->
            val candidateMap = candidates.associateBy { it.id }
            val parsed = answerParser.parse(raw, candidateMap.keys) ?: return@map null
            val normalized = answerParser.normalizeSuggestionOrNull(parsed) ?: return@map null
            if (!candidateMap.containsKey(normalized.id)) return@map null
            normalized
        }
    }
}