package com.grippo.connectivity

import com.grippo.toolkit.context.ContextModule
import com.grippo.toolkit.context.NativeContext
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [ContextModule::class])
@ComponentScan
public class ConnectivityModule {
    @Single
    internal fun provideConnectivity(nativeContext: NativeContext): Connectivity {
        return nativeContext.Connectivity()
    }
}
