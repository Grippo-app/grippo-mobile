package com.grippo.toolkit.permission.internal

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicReference

/**
 * Bridges [PermissionRequestActivity] (which holds the result) back to
 * [AndroidPermissionManager] (which is awaiting it).
 *
 * Only one permission request is expected at a time; concurrent requests are
 * not a supported use-case for a mobile app.
 */
internal data class PermissionResult(
    val granted: Boolean,
    /** `true` = can ask again; `false` = "don't ask again" was checked. */
    val canAskAgain: Boolean,
)

internal object PermissionResultHolder {
    val deferred: AtomicReference<CompletableDeferred<Map<String, PermissionResult>>?> =
        AtomicReference(null)
}
