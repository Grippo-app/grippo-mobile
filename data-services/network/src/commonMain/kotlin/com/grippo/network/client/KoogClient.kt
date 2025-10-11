package com.grippo.network.client

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import com.grippo.network.internal.ApiErrorParser
import com.grippo.network.internal.ClientLogger
import com.grippo.network.internal.responseValidator
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import org.koin.core.annotation.Single

@Single
internal class KoogClient(
    httpClient: HttpClient,
    clientLogger: ClientLogger,
    apiErrorParser: ApiErrorParser,
) {

    private val clientProvider = httpClient.config {
        install(Logging) {
            level = LogLevel.ALL
            logger = clientLogger
        }

        responseValidator(apiErrorParser)
    }

    private val model: LLModel = LLModel(
        provider = LLMProvider.OpenRouter,
        id = "qwen/qwen-2.5-7b-instruct",
        capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
        contextLength = 65_536
    )

    private val executor: PromptExecutor by lazy {
        val client = OpenRouterLLMClient(
            apiKey = "",
            baseClient = clientProvider
        )
        SingleLLMPromptExecutor(client)
    }

    suspend fun invoke(
        input: String,
        system: String
    ): String {
        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = model,
            systemPrompt = system
        )
        return agent.run(input)
    }
}