package com.grippo.core.error.provider.impl

import com.grippo.dialog.api.DialogModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DialogModule::class])
@ComponentScan
public class ErrorModule
