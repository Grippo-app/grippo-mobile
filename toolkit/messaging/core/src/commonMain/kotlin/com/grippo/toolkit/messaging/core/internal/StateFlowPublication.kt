package com.grippo.toolkit.messaging.core.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Re-publish until the flow matches canonical state, because a racing publisher can otherwise leave a stale value.
internal inline fun <T> publishState(flow: MutableStateFlow<T>, readState: () -> T) {
    do {
        flow.update { readState() }
    } while (flow.value != readState())
}
