package com.grippo.data.features.equipment

import com.grippo.services.backend.BackendModule
import com.grippo.services.database.DatabaseModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [BackendModule::class, DatabaseModule::class])
@ComponentScan
public class EquipmentFeatureModule
