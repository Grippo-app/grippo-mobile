package com.grippo.toolkit.permission

import com.grippo.toolkit.context.ContextModule
import com.grippo.toolkit.context.NativeContext
import com.grippo.toolkit.permission.internal.getPermissionManager
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [ContextModule::class])
@ComponentScan
public class PermissionManagerModule {

    @Single
    internal fun providePermissionManager(nativeContext: NativeContext): PermissionManager {
        return nativeContext.getPermissionManager()
    }
}
