package com.grippo.core

import com.grippo.core.models.ComponentIdentifier
import com.grippo.core.models.ComponentLifeCycleEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

public class ComponentLifecycleEmitter {
    private val _listener =
        Channel<Pair<ComponentIdentifier, ComponentLifeCycleEvent>>(capacity = Channel.BUFFERED)

    public val listener: Flow<Pair<ComponentIdentifier, ComponentLifeCycleEvent>> =
        _listener.receiveAsFlow()

    public fun invoke(value: ComponentIdentifier, event: ComponentLifeCycleEvent) {
        _listener.trySend(value to event)
    }
}
