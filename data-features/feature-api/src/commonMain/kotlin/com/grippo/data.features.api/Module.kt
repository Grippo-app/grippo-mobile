package com.grippo.data.features.api

import com.grippo.data.features.api.authorization.LoginUseCase
import com.grippo.data.features.api.authorization.RegisterUseCase
import com.grippo.data.features.api.user.GetUserUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

public val featureApiModule: Module = module {
    factory {
        LoginUseCase(get(), get(), get(), get())
    }
    factory {
        RegisterUseCase(get(), get(), get(), get())
    }
    factory {
        GetUserUseCase(get(), get(), get())
    }
}