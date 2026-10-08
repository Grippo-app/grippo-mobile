package com.grippo.shared.dialog

import androidx.compose.runtime.Immutable
import com.grippo.dialog.api.DialogConfig
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class DialogState(
    val session: DialogSession? = null,
    val stack: PersistentList<DialogEntry> = persistentListOf(),
    val phase: SheetPhase = SheetPhase.Released,
    val pending: DialogConfig? = null,
)

@Immutable
internal data class DialogSession(val id: String)

/** Stable runtime identity preserves child instances across stack updates. */
@Immutable
internal class DialogStep(val id: String, val config: DialogConfig) {
    override fun equals(other: Any?): Boolean = other is DialogStep && id == other.id
    override fun hashCode(): Int = id.hashCode()
}

@Immutable
internal data class DialogEntry(
    val config: DialogConfig,
    val id: String = Uuid.random().toString(),
    val pendingResult: (() -> Unit)? = null,
) {
    fun asStep(): DialogStep = DialogStep(id, config)
}

@Immutable
internal sealed class SheetPhase {
    data object Present : SheetPhase()
    data object Dismissing : SheetPhase()
    data object Released : SheetPhase()
}
