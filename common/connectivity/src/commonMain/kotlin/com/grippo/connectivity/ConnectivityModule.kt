package com.grippo.connectivity

import com.grippo.platform.context.NativeContext
import com.grippo.platform.context.PlatformModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [PlatformModule::class])
@ComponentScan
public class ConnectivityModule {
    @Single
    internal fun provideConnectivity(nativeContext: NativeContext): Connectivity {
        return nativeContext.Connectivity()
    }
}
