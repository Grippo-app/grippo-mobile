package com.grippo.data.features.excluded.muscles

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [com.grippo.services.backend.BackendModule::class, com.grippo.services.database.DatabaseModule::class])
@ComponentScan
public class ExcludedMusclesFeatureModule
