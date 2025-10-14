package com.grippo.toolkit.context

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.scope.Scope

@Module
@ComponentScan
public actual class ContextModule actual constructor() {

    @Single
    internal actual fun providesNativeContext(scope: Scope): NativeContext {
        return NativeContext()
    }
}
