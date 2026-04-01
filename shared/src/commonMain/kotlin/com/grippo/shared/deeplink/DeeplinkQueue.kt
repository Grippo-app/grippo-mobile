package com.grippo.shared.deeplink

import com.arkivanov.essenty.instancekeeper.InstanceKeeper

/**
 * Survives configuration changes via [InstanceKeeper] (retained instance).
 *
 * Holds at most one pending deeplink string. A deeplink is enqueued when the
 * app is cold-started from a notification tap (auth flow is still running) and
 * consumed inside [com.grippo.shared.root.RootViewModel.toHome] once the stack
 * has settled on the Home screen.
 */
internal class DeeplinkQueue : InstanceKeeper.Instance {

    private var pending: String? = null

    /** Store [deeplink] to be handled as soon as the user reaches Home. */
    fun enqueue(deeplink: String) {
        pending = deeplink
    }

    /** Returns and clears the pending deeplink, or `null` if none is queued. */
    fun consume(): String? = pending.also { pending = null }

    override fun onDestroy() {
        pending = null
    }
}
