package com.grippo.toolkit.http.client

import com.grippo.toolkit.context.ContextModule
import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.http.client.internal.ApiErrorParser
import com.grippo.toolkit.http.client.internal.responseValidator
import com.grippo.toolkit.serialization.SerializationModule
import io.ktor.client.HttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [ContextModule::class, SerializationModule::class])
@ComponentScan
public class HttpModule {

    @Single
    internal fun HttpClient(
        context: NativeContext,
        apiErrorParser: ApiErrorParser
    ): HttpClient {
        return context.driver().config {
            responseValidator(apiErrorParser)
        }
    }
}
