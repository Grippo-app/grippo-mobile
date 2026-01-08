package com.grippo.services.ai.agent.client

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
internal class KoogClient(httpClient: HttpClient) {

    private val clientProvider = httpClient

    private val model: LLModel = LLModel(
        provider = LLMProvider.Google,
        id = "gemini-2.0-flash-lite",
        capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
        contextLength = 1_048_576
    )

    private val executor: PromptExecutor by lazy {
        val client = GoogleLLMClient(
            apiKey = GEMINI_API_KEY,
            baseClient = clientProvider
        )
        SingleLLMPromptExecutor(client)
    }

    private companion object {
        private const val GEMINI_API_KEY = ""
    }

    suspend fun invoke(input: String, system: String): String {
        return withContext(Dispatchers.IO) {
            val agent = AIAgent.Companion(
                promptExecutor = executor,
                llmModel = model,
                systemPrompt = system
            )
            agent.run(input)
        }
    }
}
