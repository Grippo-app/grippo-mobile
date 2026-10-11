package com.grippo.toolkit.crashreporting.core

import com.grippo.toolkit.crashreporting.CrashReporting
import com.grippo.toolkit.crashreporting.CrashReportingBootstrap
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
public class CrashReportingModule {
    @Single(binds = [CrashReporting::class, CrashReportingBootstrap::class])
    internal fun provideCrashReporting(): DefaultCrashReporting = DefaultCrashReporting()
}
