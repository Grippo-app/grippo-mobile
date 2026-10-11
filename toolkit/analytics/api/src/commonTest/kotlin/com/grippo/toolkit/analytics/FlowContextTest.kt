package com.grippo.toolkit.analytics

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

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
