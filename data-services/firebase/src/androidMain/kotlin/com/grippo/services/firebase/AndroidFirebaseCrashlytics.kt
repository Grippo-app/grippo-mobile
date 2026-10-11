package com.grippo.services.firebase

import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.grippo.toolkit.crashreporting.context.CrashReportContext
import com.grippo.toolkit.crashreporting.metadata.CrashMetadataEntry
import com.grippo.toolkit.crashreporting.transport.CrashReportingSink
import com.grippo.toolkit.crashreporting.values.CrashDiagnosticCode

public class AndroidFirebaseCrashlytics(private val core: FirebaseCrashlytics) : CrashReportingSink {
    override fun logBreadcrumb(code: CrashDiagnosticCode) {
        core.log(code.value)
    }

    override fun reportException(throwable: Throwable, context: CrashReportContext) {
        if (context.metadata.isEmpty()) {
            core.recordException(throwable)
            return
        }
        val keys = CustomKeysAndValues.Builder()
        context.metadata.forEach { attribute ->
            when (attribute) {
                is CrashMetadataEntry.Code -> keys.putString(attribute.key.name, attribute.value.value)
                is CrashMetadataEntry.Count -> keys.putLong(attribute.key.name, attribute.value)
                is CrashMetadataEntry.Flag -> keys.putBoolean(attribute.key.name, attribute.value)
            }
        }
        core.recordException(throwable, keys.build())
    }
}
