package com.grippo.toolkit.browser

import com.grippo.toolkit.browser.internal.getBrowserRedirector
import com.grippo.toolkit.context.ContextModule
import com.grippo.toolkit.context.NativeContext
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [ContextModule::class])
@ComponentScan
public class BrowserModule {
    @Single
    internal fun provideBrowserRedirector(nativeContext: NativeContext): BrowserRedirector {
        return nativeContext.getBrowserRedirector()
    }
}
