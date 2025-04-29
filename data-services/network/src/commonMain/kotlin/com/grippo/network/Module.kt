package com.grippo.network

import com.grippo.network.client.NetworkClient
import com.grippo.network.client.configure
import com.grippo.platform.core.NativeContext
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

public val networkModule: Module = module {
    single {
        get<NativeContext>().driver().configure()
    }

    single {
        NetworkClient(
            httpClient = get(),
            json = get()
        )
    }

    single {
        Json {
            useAlternativeNames = false
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
        }
    }
}
