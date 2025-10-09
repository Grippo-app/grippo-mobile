package com.grippo.network

import com.grippo.database.DatabaseModule
import com.grippo.network.client.ApiErrorParser
import com.grippo.network.client.TokenProvider
import com.grippo.network.client.configure
import com.grippo.platform.core.NativeContext
import com.grippo.platform.core.PlatformModule
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [PlatformModule::class, DatabaseModule::class])
@ComponentScan
public class NetworkModule {

    @Single
    internal fun provideHttpClient(
        context: NativeContext,
        tokenProvider: TokenProvider,
        apiErrorParser: ApiErrorParser
    ): HttpClient {
        return context.driver().configure(
            tokenProvider = tokenProvider,
            apiErrorParser = apiErrorParser
        )
    }

    @Single
    internal fun provideJson(): Json {
        return Json {
            useAlternativeNames = false
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
        }
    }
}
