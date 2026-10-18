package com.grippo.toolkit.http.client.internal

import kotlin.coroutines.cancellation.CancellationException

internal fun Throwable.rethrowCancellationOrFatal() {
    val seen = mutableListOf<Throwable>()
    var current = this
    while (seen.none { it === current }) {
        seen.add(current)
        if (current is CancellationException || current !is Exception) {
            throw current
        }
        current = current.cause ?: return
    }
}
