package com.grippo.error.provider.impl

import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.error.provider.AppError
import com.grippo.error.provider.ErrorProvider
import org.koin.core.annotation.Single

@Single(binds = [ErrorProviderImpl::class])
internal class ErrorProviderImpl(val dialogController: DialogController) : ErrorProvider {

    override fun provide(exception: Throwable, callback: () -> Unit) {
        val config = when (exception) {
            is AppError.Network.NoInternet -> DialogConfig.ErrorDisplay(
                title = "No Internet",
                description = exception.message,
                onClose = callback
            )

            is AppError.Network.Timeout -> DialogConfig.ErrorDisplay(
                title = "Request Timeout",
                description = exception.message,
                onClose = callback
            )

            is AppError.Network.Expected -> DialogConfig.ErrorDisplay(
                title = exception.title,
                description = exception.description,
                onClose = callback
            )

            is AppError.Network.Unexpected -> DialogConfig.ErrorDisplay(
                title = "Server Error",
                description = exception.message,
                onClose = callback
            )

            is AppError.Unknown -> DialogConfig.ErrorDisplay(
                title = "Unknown Error",
                description = exception.message,
                onClose = callback
            )

            else -> DialogConfig.ErrorDisplay(
                title = "Unexpected Error",
                description = exception.message ?: "Something went wrong.",
                onClose = callback
            )
        }

        dialogController.show(config)
    }
}