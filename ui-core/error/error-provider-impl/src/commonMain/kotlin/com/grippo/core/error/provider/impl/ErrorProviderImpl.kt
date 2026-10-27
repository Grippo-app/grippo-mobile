package com.grippo.core.error.provider.impl

import com.grippo.core.error.provider.AppError
import com.grippo.core.error.provider.ErrorDecision
import com.grippo.core.error.provider.ErrorProvider
import com.grippo.core.state.error.AppErrorState
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import kotlinx.coroutines.CompletableDeferred
import org.koin.core.annotation.Single

@Single(binds = [ErrorProvider::class])
internal class ErrorProviderImpl(
    private val dialogController: DialogController,
) : ErrorProvider {

    override suspend fun retry(error: Throwable): ErrorDecision {
        val decision = CompletableDeferred<ErrorDecision>()

        val mode = DialogConfig.ErrorDisplay.Mode.Retry(
            onRetry = { decision.complete(ErrorDecision.Retry) },
        )

        open(
            error = error,
            mode = mode,
            onClose = { decision.complete(ErrorDecision.Close) }
        )

        return decision.await()
    }

    override suspend fun inform(error: Throwable) {
        val closed = CompletableDeferred<Unit>()

        val mode = DialogConfig.ErrorDisplay.Mode.Inform

        open(
            error = error,
            mode = mode,
            onClose = { closed.complete(Unit) }
        )

        closed.await()
    }

    private fun open(error: Throwable, mode: DialogConfig.ErrorDisplay.Mode, onClose: () -> Unit) {
        val state = when (error) {

            is AppError.Network.NoInternet -> {
                AppErrorState.Network.NoInternet
            }

            is AppError.Network.Timeout -> {
                AppErrorState.Network.Timeout
            }

            is AppError.Network.Expected -> {
                AppErrorState.Network.Expected(
                    title = error.title,
                    description = error.description,
                )
            }

            is AppError.Network.IncompleteResponse -> {
                AppErrorState.Network.IncompleteResponse
            }

            is AppError.Network.Unexpected -> {
                AppErrorState.Network.Unexpected
            }

            is AppError.Expected -> {
                AppErrorState.Expected(
                    title = error.message,
                    description = error.description,
                )
            }

            is AppError.Unknown -> {
                AppErrorState.Unknown
            }

            else -> {
                AppErrorState.Unknown
            }
        }

        val config = DialogConfig.ErrorDisplay(
            title = state.title(),
            error = state,
            mode = mode,
            onClose = onClose,
        )

        dialogController.open(config)
    }
}
