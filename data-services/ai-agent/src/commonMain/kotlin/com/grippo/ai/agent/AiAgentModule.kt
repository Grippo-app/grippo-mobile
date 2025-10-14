package com.grippo.ai.agent

import com.grippo.platform.http.client.HttpModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [HttpModule::class])
@ComponentScan
public class AiAgentModule