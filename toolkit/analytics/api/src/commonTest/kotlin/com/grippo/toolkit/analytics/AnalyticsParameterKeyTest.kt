package com.grippo.toolkit.analytics

import com.grippo.toolkit.analytics.context.ScreenId
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey
import com.grippo.toolkit.analytics.transport.AnalyticsValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AnalyticsParameterKeyTest {
    private enum class Observation {
        Selected,
        Cleared,
    }

    @Test
    fun enumCodesUseTheSuppliedEncoderAndPreserveTheirKeyContract() {
        val key = AnalyticsParameterKey.code<Observation>("interaction", required = false) {
            when (it) {
                Observation.Selected -> "chosen"
                Observation.Cleared -> "reset"
            }
        }
        assertEquals("interaction", key.name)
        assertFalse(key.required)
        val selected = key.value(Observation.Selected)
        assertSame(key, selected.key)
        assertEquals(AnalyticsValue.Text("chosen"), selected.value)
        assertEquals(AnalyticsValue.Text("reset"), key.value(Observation.Cleared).value)
    }

    @Test
    fun existingAnalyticsCodesKeepTheirEncodingAndDefaultRequiredContract() {
        val key = AnalyticsParameterKey.code<ScreenId>("screen")
        assertTrue(key.required)
        assertEquals(AnalyticsValue.Text("auth_login"), key.value(ScreenId("auth_login")).value)
    }
}
