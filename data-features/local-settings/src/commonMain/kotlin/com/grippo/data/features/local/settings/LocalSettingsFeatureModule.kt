package com.grippo.data.features.local.settings

import com.grippo.services.datastore.DataStoreModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DataStoreModule::class])
@ComponentScan
public class LocalSettingsFeatureModule
