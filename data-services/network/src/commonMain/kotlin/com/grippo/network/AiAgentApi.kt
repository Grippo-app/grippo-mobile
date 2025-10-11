package com.grippo.network

import com.grippo.network.client.KoogClient
import org.koin.core.annotation.Single

@Single
public class AiAgentApi internal constructor(private val client: KoogClient) {

    public suspend fun ask(
        input: String,
        system: String
    ): String {
        return client.invoke(input, system)
    }
}