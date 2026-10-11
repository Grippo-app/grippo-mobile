package com.grippo.toolkit.analytics.core.internal

import com.grippo.toolkit.analytics.core.AnalyticsModule
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import org.koin.core.error.InstanceCreationException
import org.koin.core.error.NoDefinitionFoundException
import org.koin.dsl.koinApplication
import org.koin.ksp.generated.module

class AnalyticsConfigurationTest {
    @Test fun missingApplicationIsAWiringErrorWithoutADefaultConfiguration() {
        val application = koinApplication {
            modules(AnalyticsModule().module)
        }
        try {
            val failure = assertFailsWith<InstanceCreationException> {
                application.koin.get<AnalyticsConfiguration>()
            }
            assertIs<NoDefinitionFoundException>(failure.cause)
        } finally {
            application.close()
        }
    }
}
