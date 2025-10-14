package com.grippo.error.provider.impl

import com.grippo.core.state.error.AppErrorState
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.error.provider.AppError
import com.grippo.error.provider.ErrorProvider
import org.koin.core.annotation.Single

@Single(binds = [ErrorProvider::class])
internal class ErrorProviderImpl(
    val dialogController: DialogController
) : ErrorProvider {

    override suspend fun provide(exception: Throwable, callback: () -> Unit) {
        val error = when (exception) {

            is AppError.Network.NoInternet -> AppErrorState.Network.NoInternet(
                description = exception.message,
            )

            is AppError.Network.Timeout -> AppErrorState.Network.Timeout(
                description = exception.message,
            )

            is AppError.Network.Expected -> AppErrorState.Network.Expected(
                title = exception.title,
                description = exception.description,
            )

            is AppError.Network.Unexpected -> AppErrorState.Network.Unexpected(
                description = exception.message,
            )

            is AppError.Expected -> AppErrorState.Expected(
                title = exception.message,
                description = exception.description,
            )

            is AppError.Unknown -> AppErrorState.Unknown

            else -> AppErrorState.Unknown
        }

        val config = DialogConfig.ErrorDisplay(
            error = error,
            onClose = callback
        )

        dialogController.show(config)
    }
}