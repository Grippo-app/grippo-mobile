package com.grippo.serialization

import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class SerializationModule {
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