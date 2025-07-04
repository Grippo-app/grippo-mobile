package com.grippo.data.features.api

import com.grippo.data.features.api.authorization.LoginUseCase
import com.grippo.data.features.api.authorization.RegisterUseCase
import org.koin.core.annotation.Module
import org.koin.dsl.module
import kotlin.jvm.JvmName

@Module
public class FeatureApiModule {

    @get:JvmName("module")
    public val module: org.koin.core.module.Module = module {
        single {
            LoginUseCase(
                authorizationFeature = get(),
                userFeature = get(),
                excludedMusclesFeature = get(),
                excludedEquipmentsFeature = get(),
                exerciseExampleFeature = get()
            )
        }

        single {
            RegisterUseCase(
                authorizationFeature = get(),
                userFeature = get(),
                excludedMusclesFeature = get(),
                excludedEquipmentsFeature = get(),
                exerciseExampleFeature = get()
            )
        }
    }
}