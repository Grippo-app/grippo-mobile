package com.grippo.toolkit.analytics.core.internal

import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.AnalyticsBootstrap
import com.grippo.toolkit.analytics.AnalyticsCollection
import com.grippo.toolkit.analytics.AnalyticsEvent
import com.grippo.toolkit.analytics.AnalyticsParameter
import com.grippo.toolkit.analytics.EventDefinition
import com.grippo.toolkit.analytics.core.AnalyticsModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.ksp.generated.module

class AnalyticsModuleTest {
    @Test fun moduleHonorsDebugAndReleaseConfiguration(): Unit = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val event = object : AnalyticsEvent {
                override val definition = EventDefinition("unregistered_test", 1, emptyList())
                override fun parameters() = emptyList<AnalyticsParameter>()
            }
            for (debug in listOf(true, false)) {
                val application = koinApplication {
                    modules(AnalyticsModule().module, module { single { AnalyticsConfiguration(debug = debug) } })
                }
                val engine = application.koin.get<DefaultAnalytics>()
                try {
                    engine.updateCollection(AnalyticsCollection.Granted)
                    if (debug) {
                        assertFailsWith<IllegalArgumentException> { engine.track(event) }
                        assertEquals(AnalyticsDisposition.InvalidEvent, engine.diagnostics().last().disposition)
                    } else {
                        engine.track(event)
                        assertEquals(emptyList(), engine.diagnostics())
                    }
                } finally {
                    engine.close()
                    application.close()
                }
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun consumerAndBootstrapShareOneEngine(): Unit = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val application = koinApplication {
                modules(AnalyticsModule().module, module { single { AnalyticsConfiguration(debug = false) } })
            }
            try {
                val engine = application.koin.get<DefaultAnalytics>()
                assertSame(engine, application.koin.get<Analytics>())
                assertSame(engine, application.koin.get<AnalyticsBootstrap>())
                engine.close()
            } finally { application.close() }
        } finally { Dispatchers.resetMain() }
    }
}
