package com.grippo.toolkit.crashreporting.transport

import com.grippo.toolkit.crashreporting.context.CrashReportContext
import com.grippo.toolkit.crashreporting.values.CrashDiagnosticCode

public interface CrashReportingSink {
    public fun reportException(throwable: Throwable, context: CrashReportContext)
    public fun logBreadcrumb(code: CrashDiagnosticCode)
}
