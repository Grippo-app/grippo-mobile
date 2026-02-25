package com.grippo.toolkit.link.opener

import com.grippo.toolkit.context.ContextModule
import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.link.opener.internal.getLinkOpener
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [ContextModule::class])
@ComponentScan
public class LinkOpenerModule {
    @Single
    internal fun provideLinkOpener(nativeContext: NativeContext): LinkOpener {
        return nativeContext.getLinkOpener()
    }
}
