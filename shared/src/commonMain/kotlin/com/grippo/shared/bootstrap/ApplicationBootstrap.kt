package com.grippo.shared.bootstrap

import com.grippo.toolkit.analytics.lifecycle.AnalyticsBootstrap
import com.grippo.toolkit.crashreporting.lifecycle.CrashReportingBootstrap
import com.grippo.toolkit.messaging.lifecycle.MessagingBootstrap

/** Instance-bound startup dependencies. Contains no global container lookup or SDK logic. */
public class ApplicationBootstrap internal constructor(
    public val analytics: AnalyticsBootstrap,
    public val crashReporting: CrashReportingBootstrap,
    public val messaging: MessagingBootstrap,
)
