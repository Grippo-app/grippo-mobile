package com.grippo.ai

import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class AiModule {

    @Single
    internal fun provideAiService(executor: PromptExecutor, model: LLModel): AiService {
        return AiService(
            executor = executor,
            model = model
        )
    }

    @Single
    internal fun provideOpenAIClient(): OpenAILLMClient {
        val settings = OpenAIClientSettings(
            baseUrl = "https://openrouter.ai/api/v1",
            chatCompletionsPath = "chat/completions",
            responsesAPIPath = "responses",
            embeddingsPath = "embeddings",
            moderationsPath = "moderations"
        )
        return OpenAILLMClient(
            apiKey = "",
            settings = settings
        )
    }

    @Single
    internal fun provideLLModel(): LLModel {
        return LLModel(
            provider = LLMProvider.OpenRouter,
            id = "deepseek/deepseek-chat-v3.1:free",
            capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
            contextLength = 128_000
        )
    }

    @Single
    internal fun providePromptExecutor(client: OpenAILLMClient): PromptExecutor {
        return SingleLLMPromptExecutor(
            llmClient = client
        )
    }
}
