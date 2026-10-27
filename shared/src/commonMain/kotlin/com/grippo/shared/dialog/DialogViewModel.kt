package com.grippo.shared.dialog

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogProvider
import com.grippo.shared.dialog.internal.SessionDialogNavigation
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
            .safeLaunch(processing = Processing.Infinity, recovery = ErrorRecovery.Retry)
    }

    private fun open(config: DialogConfig) {
        update { current ->
            when (current.phase) {
                SheetPhase.Released -> current.copy(
                    session = DialogSession(id = Uuid.random().toString()),
                    stack = persistentListOf(DialogEntry(config = config)),
                    phase = SheetPhase.Present,
                )

                SheetPhase.Present -> {
                    if (current.stack.any { it.config.hasSameContentAs(config) }) {
                        return@update current
                    }
                    current.copy(stack = current.stack.adding(DialogEntry(config = config)))
                }

                SheetPhase.Dismissing -> {
                    if (current.pending.any { it.hasSameContentAs(config) }) {
                        return@update current
                    }
                    current.copy(pending = current.pending.adding(config))
                }
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

        val nextState = if (current.pending.isEmpty()) {
            DialogState()
        } else {
            DialogState(
                session = DialogSession(id = Uuid.random().toString()),
                stack = current.pending.map { DialogEntry(config = it) }.toPersistentList(),
                phase = SheetPhase.Present,
            )
        }

        // Invalidate old navigation before callbacks can open or modify another session.
        update { nextState }
        current.stack.asReversed().forEach { it.config.onDismiss?.invoke() }
        current.stack.lastOrNull()?.pendingResult?.invoke()
    }
}
