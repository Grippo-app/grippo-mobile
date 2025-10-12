package com.grippo.backend

import com.grippo.database.DatabaseModule
import com.grippo.http.client.HttpModule
import com.grippo.platform.core.PlatformModule
import com.grippo.serialization.SerializationModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [PlatformModule::class, HttpModule::class, DatabaseModule::class, SerializationModule::class])
@ComponentScan
public class NetworkModule