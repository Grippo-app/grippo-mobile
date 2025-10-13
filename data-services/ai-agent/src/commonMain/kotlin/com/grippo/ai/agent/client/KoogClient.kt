package com.grippo.ai.agent.client

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
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
        provider = LLMProvider.OpenRouter,
        id = "qwen/qwen-2.5-7b-instruct",
        capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
        contextLength = 65_536
    )

    private val executor: PromptExecutor by lazy {
        val client = OpenRouterLLMClient(
            apiKey = "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",
            baseClient = clientProvider
        )
        SingleLLMPromptExecutor(client)
    }

    suspend fun invoke(
        input: String,
        system: String
    ): String {
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