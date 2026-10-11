package com.grippo.toolkit.analytics.core.internal

import com.grippo.toolkit.analytics.core.AnalyticsModule
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.koin.dsl.koinApplication
import org.koin.ksp.generated.module

class AnalyticsConfigurationTest {
    @OptIn(ExperimentalNativeApi::class)
    @Test fun configurationUsesTheNativeBuildModeAndIsASingleton() {
        val application = koinApplication {
            modules(AnalyticsModule().module)
        }
        try {
            val configuration = application.koin.get<AnalyticsConfiguration>()
            assertEquals(Platform.isDebugBinary, configuration.debug)
            assertSame(configuration, application.koin.get<AnalyticsConfiguration>())
        } finally {
            application.close()
        }
    }
}
