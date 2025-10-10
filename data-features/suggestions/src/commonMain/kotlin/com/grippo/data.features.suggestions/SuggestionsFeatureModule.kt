package com.grippo.data.features.suggestions

import com.grippo.ai.AiModule
import com.grippo.database.DatabaseModule
import com.grippo.serialization.SerializationModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [AiModule::class, DatabaseModule::class, SerializationModule::class])
@ComponentScan
public class SuggestionsFeatureModule
