package com.grippo.core.internal

import com.grippo.core.models.ComponentLifeCycleEvent
import com.grippo.logger.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@OptIn(FlowPreview::class)
internal object ComponentTreeLogger {
    private val activeComponents = mutableListOf<String>()
    private val eventChannel = Channel<Unit>(Channel.CONFLATED)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        eventChannel.receiveAsFlow()
            .debounce(1000)
            .onEach { printNavigationTree() }
            .launchIn(coroutineScope)
    }

    fun logLifecycleEvent(event: ComponentLifeCycleEvent, value: String?) {
        when (event) {
            ComponentLifeCycleEvent.OnCreate -> activeComponents.add(value ?: "Unknown")
            ComponentLifeCycleEvent.OnDestroy -> activeComponents.remove(value)
            else -> {}
        }
        eventChannel.trySend(Unit)
    }

    private fun printNavigationTree() {
        val tree = buildString {
            appendLine("┌────── 📦 COMPONENT TREE ──────")
            activeComponents.forEachIndexed { index, name ->
                val indent = "│   ".repeat(index)
                val branch = if (index == activeComponents.lastIndex) "└── " else "├── "
                appendLine("$indent$branch$name")
            }
            append("└" + "─".repeat(30))
        }
        AppLogger.Navigation.log(tree)
    }
}
