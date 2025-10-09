package com.grippo.data.features.weight.history

import com.grippo.database.DatabaseModule
import com.grippo.network.NetworkModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [NetworkModule::class, DatabaseModule::class])
@ComponentScan
public class WeightHistoryFeatureModule
