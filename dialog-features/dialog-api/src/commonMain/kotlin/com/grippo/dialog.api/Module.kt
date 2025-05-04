package com.grippo.dialog.api

import com.grippo.dialog.api.internal.DialogControllerImpl
import org.koin.core.module.Module
import org.koin.dsl.module

public val dialogModule: Module = module {
    single<DialogController> { DialogControllerImpl() }
}