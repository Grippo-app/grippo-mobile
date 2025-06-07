package com.grippo.network

import com.grippo.network.client.ApiErrorParser
import com.grippo.network.client.NetworkClient
import com.grippo.network.client.TokenProvider
import com.grippo.network.client.configure
import com.grippo.platform.core.NativeContext
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

public val networkModule: Module = module {
    single {
        get<NativeContext>().driver().configure(get(), get())
    }

    single {
        NetworkClient(
            httpClient = get(),
            json = get(),
        )
    }

    single {
        Api(client = get())
    }

    single {
        TokenProvider(tokenDao = get(), userActiveDao = get())
    }

    single {
        ApiErrorParser(json = get())
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
