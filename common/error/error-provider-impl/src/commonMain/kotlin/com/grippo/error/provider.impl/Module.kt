package com.grippo.error.provider.impl

import com.grippo.error.provider.ErrorProvider
import org.koin.core.module.Module
import org.koin.dsl.module

public val errorModule: Module = module {
    single<ErrorProvider> {
        ErrorProviderImpl(get())
    }
}