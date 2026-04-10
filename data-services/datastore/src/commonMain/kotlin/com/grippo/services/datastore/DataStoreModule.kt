package com.grippo.services.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.grippo.toolkit.context.ContextModule
import com.grippo.toolkit.context.NativeContext
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [ContextModule::class])
@ComponentScan
public class DataStoreModule {

    @Single
    internal fun providePreferencesDataStore(
        nativeContext: NativeContext
    ): DataStore<Preferences> {
        return nativeContext.getPreferencesDataStore()
    }
}
