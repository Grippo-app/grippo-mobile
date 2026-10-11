package com.grippo.toolkit.analytics.context

import com.grippo.toolkit.analytics.internal.opaqueIdPattern
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
public data class OperationId(public val value: String) {
    init {
        require(opaqueIdPattern.matches(value))
    }

    public companion object {
        public fun create(): OperationId = OperationId(Uuid.random().toString())
    }
}
