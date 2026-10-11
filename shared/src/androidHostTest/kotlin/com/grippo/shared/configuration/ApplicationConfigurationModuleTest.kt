package com.grippo.shared.configuration

import com.grippo.toolkit.analytics.core.AnalyticsConfiguration
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import org.koin.core.error.InstanceCreationException
import org.koin.core.error.NoDefinitionFoundException
import org.koin.dsl.koinApplication
import org.koin.ksp.generated.module

class ApplicationConfigurationModuleTest {
    @Test fun missingApplicationIsAWiringErrorWithoutADefaultConfiguration() {
        val application = koinApplication {
            modules(ApplicationConfigurationModule().module)
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
