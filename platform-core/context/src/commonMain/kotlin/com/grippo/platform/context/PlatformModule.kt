package com.grippo.platform.context

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.scope.Scope

@Module
@ComponentScan
public expect class PlatformModule() {

    @Single
    internal fun providesNativeContext(scope: Scope): NativeContext
}
