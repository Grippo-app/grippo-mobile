package com.grippo.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel

public class AiService(
    private val executor: PromptExecutor,
    private val model: LLModel,
) {
    public suspend fun ask(
        input: String,
        system: String? = null
    ): String {
        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = model,
            systemPrompt = system ?: DEFAULT_SYSTEM
        )
        return agent.run(input)
    }

    private companion object {
        private const val DEFAULT_SYSTEM = "You are a concise assistant"
    }
}