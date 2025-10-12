package com.grippo.data.features.suggestions

import com.grippo.agent.AiAgentModule
import com.grippo.database.DatabaseModule
import com.grippo.serialization.SerializationModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [AiAgentModule::class, DatabaseModule::class, SerializationModule::class])
@ComponentScan
public class AiSuggestionsFeatureModule