package com.grippo.analytics.authorization.internal.state

import com.grippo.analytics.authorization.internal.schema.AuthEntryPoints
import com.grippo.analytics.authorization.internal.schema.AuthScreen
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.NavigationReason

internal class AuthSessionFlow(restored: FlowContext?) {
    val context = restored ?: FlowContext.start(AuthEntryPoints.SessionCheck, null)

    private var reason = if (restored == null) {
        NavigationReason.Initial
    } else {
        NavigationReason.Restore
    }

    fun navigation(): NavigationContext =
        NavigationContext(AuthScreen.Splash, reason = reason).also {
            reason = NavigationReason.Restore
        }
}
