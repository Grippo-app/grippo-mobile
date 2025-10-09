package com.grippo.data.features.suggestions

import com.grippo.ai.AiModule
import com.grippo.database.DatabaseModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [AiModule::class, DatabaseModule::class])
@ComponentScan
public class SuggestionsFeatureModule
