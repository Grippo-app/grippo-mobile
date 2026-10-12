package com.grippo.analytics.common

import com.grippo.analytics.common.models.ControlAction
import com.grippo.analytics.common.models.InputKeyboardAction
import com.grippo.analytics.common.models.InputStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonInteractionModelsTest {
    @Test
    fun sharedCodesKeepTheirExistingWireValues() {
        assertEquals(listOf("empty", "invalid", "valid"), InputStatus.entries.map { it.analyticsCode })
        assertEquals(listOf("next", "done"), InputKeyboardAction.entries.map { it.analyticsCode })
        assertEquals(listOf("back", "next"), ControlAction.entries.map { it.analyticsCode })
    }
}
