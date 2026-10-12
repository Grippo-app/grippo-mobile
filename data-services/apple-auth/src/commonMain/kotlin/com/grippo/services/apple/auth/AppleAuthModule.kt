package com.grippo.services.apple.auth

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
public class AppleAuthModule {
    @Single
    internal fun provideAppleAuthProvider(): AppleAuthProvider = AppleAuthProvider()
}
