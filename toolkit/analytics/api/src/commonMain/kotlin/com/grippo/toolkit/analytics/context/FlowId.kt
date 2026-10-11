package com.grippo.toolkit.analytics.context

import com.grippo.toolkit.analytics.internal.opaqueIdPattern
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
public data class FlowId(public val value: String) {
    init {
        require(opaqueIdPattern.matches(value))
    }

    public companion object {
        public fun create(): FlowId = FlowId(Uuid.random().toString())
    }
}
