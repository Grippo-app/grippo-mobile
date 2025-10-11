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