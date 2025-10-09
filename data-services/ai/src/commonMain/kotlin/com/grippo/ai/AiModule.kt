package com.grippo.ai

import ai.koog.prompt.executor.llms.all.simpleOpenRouterExecutor
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
    internal fun provideLLModel(): LLModel {
        return LLModel(
            provider = LLMProvider.OpenRouter,
            id = "qwen/qwen-2.5-7b-instruct", // stable & ultra-cheap
            capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
            contextLength = 65_536
        )
    }

    @Single
    internal fun providePromptExecutor(): PromptExecutor =
        simpleOpenRouterExecutor(
            apiKey = ""
        )
}
