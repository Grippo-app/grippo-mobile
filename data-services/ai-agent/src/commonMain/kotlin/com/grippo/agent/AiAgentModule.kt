package com.grippo.agent

import com.grippo.http.client.HttpModule
import com.grippo.platform.core.PlatformModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [PlatformModule::class, HttpModule::class])
@ComponentScan
public class AiAgentModule