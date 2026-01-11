package com.grippo.services.backend

import com.grippo.services.database.DatabaseModule
import com.grippo.toolkit.http.client.HttpModule
import com.grippo.toolkit.serialization.SerializationModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [HttpModule::class, DatabaseModule::class, SerializationModule::class])
@ComponentScan
public class BackendModule