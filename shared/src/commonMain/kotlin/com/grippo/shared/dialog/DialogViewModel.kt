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
        if (current.phase != SheetPhase.Present) return
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
        if (state.value.phase != SheetPhase.Present) return
        update { it.copy(phase = SheetPhase.Dismissing) }
    }

    override fun onRelease(session: DialogSession) {
        val current = state.value
        if (current.session?.id != session.id || current.phase == SheetPhase.Released) return
        // Invalidate old navigation before callbacks can open or modify another session.
        update { DialogState() }
        current.pending?.let(::open)
        current.stack.asReversed().forEach { it.config.onDismiss?.invoke() }
        current.stack.lastOrNull()?.pendingResult?.invoke()
    }

    private fun open(config: DialogConfig) {
        val current = state.value
        if (current.phase != SheetPhase.Released) {
            // A new opening is a new session, never an implicit in-sheet push.
            update { it.copy(pending = config, phase = SheetPhase.Dismissing) }
        } else {
            update {
                DialogState(
                    session = DialogSession(id = Uuid.random().toString()),
                    stack = persistentListOf(DialogEntry(config)),
                    phase = SheetPhase.Present,
                )
            }
        }
    }
}
