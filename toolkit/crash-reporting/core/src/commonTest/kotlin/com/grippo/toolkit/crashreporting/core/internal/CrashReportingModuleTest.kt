package com.grippo.toolkit.crashreporting.core.internal

import com.grippo.toolkit.crashreporting.CrashReporting
import com.grippo.toolkit.crashreporting.CrashReportingBootstrap
import com.grippo.toolkit.crashreporting.core.CrashReportingModule
import kotlin.test.Test
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.ksp.generated.module

class CrashReportingModuleTest {
    @Test fun consumerAndBootstrapShareOneEngine() = runTest {
        val application = koinApplication { modules(CrashReportingModule().module) }
        val engine = application.koin.get<DefaultCrashReporting>()
        try {
            assertSame(engine, application.koin.get<CrashReporting>())
            assertSame(engine, application.koin.get<CrashReportingBootstrap>())
        } finally {
            engine.close()
            application.close()
        }
    }
}
