package com.grippo.toolkit.crashreporting.transport

import com.grippo.toolkit.crashreporting.context.CrashReportContext
import com.grippo.toolkit.crashreporting.values.CrashDiagnosticCode

/** SDK-only boundary; synchronous submission, not remote delivery. Core invokes on its background worker; local SDK persistence may block. No core callbacks. */
public interface CrashReportingSink {
    public fun reportException(throwable: Throwable, context: CrashReportContext)
    public fun logBreadcrumb(code: CrashDiagnosticCode)
}
