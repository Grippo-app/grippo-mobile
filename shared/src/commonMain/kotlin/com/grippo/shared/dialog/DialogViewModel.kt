package com.grippo.shared.dialog

import com.grippo.core.foundation.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogProvider
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

internal class DialogViewModel(
    dialogProvider: DialogProvider,
) : BaseViewModel<DialogState, DialogDirection, DialogLoader>(DialogState()), DialogContract {

    init {
        dialogProvider.openings
            .onEach(::open)
            .safeLaunch(processing = Processing.Infinity)
    }

    private fun open(config: DialogConfig) {
        update { current ->
            if (current.phase == SheetPhase.Released) {
                current.copy(
                    session = DialogSession(id = Uuid.random().toString()),
                    stack = persistentListOf(DialogEntry(config)),
                    phase = SheetPhase.Present,
                )
            } else {
                val pending = if (config is DialogConfig.ErrorDisplay) current.pending else config
                val pendingErrors = if (config is DialogConfig.ErrorDisplay) {
                    current.pendingErrors.add(config)
                } else {
                    current.pendingErrors
                }
                val phase = if (current.stack.lastOrNull()?.config is DialogConfig.ErrorDisplay) {
                    current.phase
                } else {
                    SheetPhase.Dismissing
                }

                current.copy(pending = pending, pendingErrors = pendingErrors, phase = phase)
            }
        }
    }

    fun navigation(session: DialogSession, owner: DialogStep): SessionDialogNavigation =
        SessionDialogNavigation(
            sessionId = session.id,
            ownerId = owner.id,
            currentState = { state.value },
            updateState = ::update,
            onFinish = ::onDismiss,
            onClose = ::onClose,
        )

    override fun onDismiss(pendingResult: (() -> Unit)?) {
        val current = state.value
        if (current.phase != SheetPhase.Present) {
            return
        }
        val top = current.stack.lastOrNull() ?: return
        if (current.stack.size == 1) {
            update {
                it.copy(
                    stack = persistentListOf(top.copy(pendingResult = pendingResult)),
                    phase = SheetPhase.Dismissing,
                )
            }
        } else {
            update { it.copy(stack = it.stack.dropLast(1).toPersistentList()) }
            top.config.onDismiss?.invoke()
            pendingResult?.invoke()
        }
    }

    override fun onClose() {
        if (state.value.phase != SheetPhase.Present) {
            return
        }
        update { it.copy(phase = SheetPhase.Dismissing) }
    }

    override fun onRelease(session: DialogSession) {
        val current = state.value
        if (current.session?.id != session.id || current.phase == SheetPhase.Released) {
            return
        }

        val nextError = current.pendingErrors.firstOrNull()
        val nextConfig = nextError ?: current.pending
        val pending = if (nextError == null) null else current.pending
        val pendingErrors = if (nextError == null) {
            current.pendingErrors
        } else {
            current.pendingErrors.removeAt(0)
        }
        val nextState = if (nextConfig == null) {
            DialogState()
        } else {
            DialogState(
                session = DialogSession(id = Uuid.random().toString()),
                stack = persistentListOf(DialogEntry(nextConfig)),
                phase = SheetPhase.Present,
                pending = pending,
                pendingErrors = pendingErrors,
            )
        }

        // Invalidate old navigation before callbacks can open or modify another session.
        update { nextState }
        current.stack.asReversed().forEach { it.config.onDismiss?.invoke() }
        current.stack.lastOrNull()?.pendingResult?.invoke()
    }
}
