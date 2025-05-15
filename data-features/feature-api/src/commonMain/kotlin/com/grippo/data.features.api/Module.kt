package com.grippo.data.features.api

import com.grippo.data.features.api.authorization.LoginUseCase
import com.grippo.data.features.api.authorization.RegisterUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

public val featureApiModule: Module = module {
    factory {
        LoginUseCase(get(), get())
    }
    factory {
        RegisterUseCase(get(), get())
    }
}