package com.grippo.toolkit.share

import com.grippo.toolkit.context.ContextModule
import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.share.internal.getImageSharer
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [ContextModule::class])
public class ShareModule {
    @Single
    internal fun provideImageSharer(nativeContext: NativeContext): ImageSharer {
        return nativeContext.getImageSharer()
    }
}
