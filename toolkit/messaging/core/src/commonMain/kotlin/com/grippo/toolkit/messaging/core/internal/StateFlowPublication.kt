package com.grippo.toolkit.messaging.core.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Publish outside the state lock, then reconcile changes hidden by an equal-value CAS (ABA). */
internal inline fun <T> publishState(flow: MutableStateFlow<T>, readState: () -> T) {
    do {
        flow.update { readState() }
    } while (flow.value != readState())
}
