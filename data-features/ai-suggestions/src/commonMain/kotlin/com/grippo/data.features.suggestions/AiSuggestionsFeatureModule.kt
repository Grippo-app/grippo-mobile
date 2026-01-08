package com.grippo.data.features.suggestions

import com.grippo.services.ai.agent.AiAgentModule
import com.grippo.toolkit.serialization.SerializationModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [AiAgentModule::class, com.grippo.services.database.DatabaseModule::class, SerializationModule::class])
@ComponentScan
public class AiSuggestionsFeatureModule