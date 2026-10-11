package com.grippo.toolkit.crashreporting

import com.grippo.toolkit.crashreporting.context.CrashReportContext
import com.grippo.toolkit.crashreporting.values.CrashDiagnosticCode

/** Nonfatal errors retain their native stack. Cancellation is never a crash report. */
public interface CrashReporting {
    public fun reportException(throwable: Throwable, context: CrashReportContext = CrashReportContext.Empty)
    public fun logBreadcrumb(code: CrashDiagnosticCode)
}
