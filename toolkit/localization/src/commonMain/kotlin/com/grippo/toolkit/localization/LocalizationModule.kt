package com.grippo.toolkit.localization

import com.grippo.toolkit.context.ContextModule
import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.localization.internal.AppLocaleObserver
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [ContextModule::class])
public class LocalizationModule {
    @Single(createdAtStart = true)
    internal fun provideAppLocaleObserver(nativeContext: NativeContext): AppLocaleObserver {
        return AppLocaleObserver(nativeContext)
    }
}
