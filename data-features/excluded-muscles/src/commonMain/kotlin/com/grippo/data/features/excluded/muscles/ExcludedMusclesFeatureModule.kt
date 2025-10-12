package com.grippo.data.features.excluded.muscles

import com.grippo.backend.NetworkModule
import com.grippo.database.DatabaseModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [NetworkModule::class, DatabaseModule::class])
@ComponentScan
public class ExcludedMusclesFeatureModule
