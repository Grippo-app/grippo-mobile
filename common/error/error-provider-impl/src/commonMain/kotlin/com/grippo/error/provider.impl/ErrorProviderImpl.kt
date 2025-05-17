package com.grippo.error.provider.impl

import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.error.provider.ErrorProvider

internal class ErrorProviderImpl(val dialogController: DialogController) : ErrorProvider {
    override fun display(title: String, description: String) {
        val config = DialogConfig.ErrorDisplay(
            title = title,
            description = description,
            onResult = {}
        )
        dialogController.show(config)
    }
}