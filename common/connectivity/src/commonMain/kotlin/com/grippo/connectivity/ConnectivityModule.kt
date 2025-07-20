package com.grippo.connectivity

import com.grippo.platform.core.NativeContext
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.grippo.connectivity")
public class ConnectivityModule {

    @Single
    internal fun provideConnectivity(nativeContext: NativeContext): Connectivity {
        return nativeContext.Connectivity()
    }
}