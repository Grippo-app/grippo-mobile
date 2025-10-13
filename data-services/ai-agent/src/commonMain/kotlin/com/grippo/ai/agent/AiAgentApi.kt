package com.grippo.ai.agent

import com.grippo.ai.agent.client.KoogClient
import org.koin.core.annotation.Single

@Single
public class AiAgentApi internal constructor(private val client: KoogClient) {

    public suspend fun ask(input: String, system: String): Result<String> {
        return runCatching { client.invoke(input, system) }
    }
}