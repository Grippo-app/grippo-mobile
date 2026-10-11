package com.grippo.toolkit.analytics

import com.grippo.toolkit.analytics.context.EntryPoint
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.FlowId
import com.grippo.toolkit.analytics.context.ScreenId
import kotlin.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FlowContextTest {
    @Test fun flowSurvivesSerializationWithoutChangingOrigin() {
        val value = FlowContext(FlowId("flow1"), EntryPoint("home_start"), ScreenId("home"))
        assertEquals(value, Json.decodeFromString<FlowContext>(Json.encodeToString(value)))
    }

    @Test fun restoredInvalidIdentifiersAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString<FlowContext>("""{"id":{"value":"email@example.com"},"entryPoint":{"analyticsCode":"home_start"},"originScreen":null}""")
        }
    }
}
