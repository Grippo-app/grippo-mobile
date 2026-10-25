package com.grippo.shared.dialog.internal

import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController.Session
import com.grippo.shared.dialog.DialogEntry
import com.grippo.shared.dialog.DialogState
import com.grippo.shared.dialog.SheetPhase
import kotlinx.collections.immutable.toPersistentList

internal class SessionDialogNavigation(
    private val sessionId: String,
    private val ownerId: String,
    private val currentState: () -> DialogState,
    private val updateState: ((DialogState) -> DialogState) -> Unit,
    private val onFinish: ((() -> Unit)?) -> Unit,
    private val onClose: () -> Unit,
) : Session {
    private fun stepIsActive(): Boolean {
        val state = currentState()
        return state.session?.id == sessionId &&
            state.phase == SheetPhase.Present &&
            state.stack.lastOrNull()?.id == ownerId
    }

    override fun push(config: DialogConfig) {
        if (!stepIsActive()) {
            return
        }
        if (currentState().stack.any { it.config.hasSameContentAs(config) }) {
            return
        }
        updateState { it.copy(stack = it.stack.adding(DialogEntry(config))) }
    }

    override fun replaceCurrent(config: DialogConfig) {
        if (!stepIsActive()) {
            return
        }
        val current = currentState().stack.lastOrNull() ?: return
        if (current.config == config) {
            return
        }
        if (currentState().stack.dropLast(1).any { it.config.hasSameContentAs(config) }) {
            return
        }
        updateState { it.copy(stack = it.stack.dropLast(1).toPersistentList().adding(DialogEntry(config))) }
        current.config.onDismiss?.invoke()
    }

    override fun back() {
        finish(null)
    }

    fun finish(pendingResult: (() -> Unit)?) {
        if (stepIsActive()) {
            onFinish(pendingResult)
        }
    }

    override fun close() {
        if (stepIsActive()) {
            onClose()
        }
    }
}
