package com.grippo.data.features.equipment

import com.grippo.backend.BackendModule
import com.grippo.database.DatabaseModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [BackendModule::class, DatabaseModule::class])
@ComponentScan
public class EquipmentFeatureModule
