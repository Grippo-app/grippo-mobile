package com.grippo.core

import org.koin.core.module.Module
import org.koin.dsl.module

public val coreModule: Module = module {
    single { ComponentLifecycleEmitter() }
}
