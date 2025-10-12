package com.grippo.http.client

import com.grippo.http.client.internal.ApiErrorParser
import com.grippo.http.client.internal.ClientLogger
import com.grippo.http.client.internal.responseValidator
import com.grippo.platform.core.NativeContext
import com.grippo.platform.core.PlatformModule
import com.grippo.serialization.SerializationModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [PlatformModule::class, SerializationModule::class])
@ComponentScan
public class HttpModule {

    @Single
    internal fun HttpClient(
        context: NativeContext,
        clientLogger: ClientLogger,
        apiErrorParser: ApiErrorParser
    ): HttpClient {
        return context.driver().config {
            install(Logging) {
                level = LogLevel.ALL
                logger = clientLogger
            }

            responseValidator(apiErrorParser)
        }
    }
}
