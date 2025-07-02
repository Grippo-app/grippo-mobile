package com.grippo.platform.core

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.scope.Scope

@Module
public expect class PlatformModule() {

    @Single
    internal  fun providesNativeContext(scope: Scope): NativeContext
}