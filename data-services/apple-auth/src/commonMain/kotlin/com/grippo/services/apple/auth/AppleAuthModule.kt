package com.grippo.services.apple.auth

import com.grippo.toolkit.context.ContextModule
import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.http.client.HttpModule
import com.grippo.toolkit.serialization.SerializationModule
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [ContextModule::class, HttpModule::class, SerializationModule::class])
public class AppleAuthModule {

    @Single
    internal fun providesAppleAuthProvider(
        nativeContext: NativeContext,
        httpClient: HttpClient,
        json: Json,
    ): AppleAuthProvider {
        return AppleAuthProvider(nativeContext, httpClient, json)
    }
}
