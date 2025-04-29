package com.grippo.shared

import com.grippo.platform.core.NativeContext
import org.koin.dsl.module

internal actual val platformModule = module {
    single { NativeContext() }
}