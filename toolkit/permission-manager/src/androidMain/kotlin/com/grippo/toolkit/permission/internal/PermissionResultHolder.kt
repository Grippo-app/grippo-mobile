package com.grippo.toolkit.permission.internal

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicReference

internal data class PermissionResult(
    val granted: Boolean,
    val canAskAgain: Boolean,
)

internal object PermissionResultHolder {
    val deferred: AtomicReference<CompletableDeferred<Map<String, PermissionResult>>?> =
        AtomicReference(null)
}
