package com.grippo.network

import com.grippo.database.DatabaseModule
import com.grippo.network.internal.ApiErrorParser
import com.grippo.network.internal.TokenProvider
import com.grippo.platform.core.NativeContext
import com.grippo.platform.core.PlatformModule
import com.grippo.serialization.SerializationModule
import io.ktor.client.HttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [PlatformModule::class, DatabaseModule::class, SerializationModule::class])
@ComponentScan
public class NetworkModule {

    @Single
    internal fun HttpClient(
        context: NativeContext,
        tokenProvider: TokenProvider,
        apiErrorParser: ApiErrorParser
    ): HttpClient {
        return context.driver()
    }
}
