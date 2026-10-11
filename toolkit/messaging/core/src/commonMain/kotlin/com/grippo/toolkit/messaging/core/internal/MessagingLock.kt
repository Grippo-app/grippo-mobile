package com.grippo.toolkit.messaging.core.internal

internal expect class MessagingLock() {
    fun <T> withLock(block: () -> T): T
}
