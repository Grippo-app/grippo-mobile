package com.grippo.toolkit.analytics.context

import kotlinx.serialization.Serializable

/** Keep this with the flow/route/draft, rather than a global last-entry-point. */
@Serializable
public data class FlowContext(
    public val id: FlowId,
    public val entryPoint: EntryPoint,
    public val originScreen: ScreenId?,
) {
    public companion object {
        /** Create once at the real entry point, then carry this context through the scenario. */
        public fun start(entryPoint: EntryPoint, originScreen: ScreenId?): FlowContext =
            FlowContext(FlowId.create(), entryPoint, originScreen)
    }
}
