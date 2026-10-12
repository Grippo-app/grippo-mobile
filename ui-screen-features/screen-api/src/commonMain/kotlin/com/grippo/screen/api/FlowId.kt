package com.grippo.screen.api

import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Stable navigation scenario identity, independent of telemetry. */
@Serializable
// Preserve the former serializer name for saved navigation state.
@SerialName("com.grippo.toolkit.analytics.context.FlowId")
public data class FlowId(public val value: String) {
    init {
        require(value.matches(VALID_ID))
    }

    public companion object {
        private val VALID_ID = Regex("[A-Za-z0-9_-]{1,64}")
        public fun create(): FlowId = FlowId(Uuid.random().toString())
    }
}
